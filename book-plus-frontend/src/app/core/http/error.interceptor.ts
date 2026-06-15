import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthStore } from '@core/auth/auth.store';
import { SKIP_AUTH } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail, isProblemDetail } from '@core/models/problem-detail.model';

// Shared across requests so concurrent 401s trigger a single refresh.
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/**
 * Centralizes HTTP error handling:
 *  - 401 on an authenticated request → try a one-shot token refresh, then retry.
 *  - Otherwise → surface a human-friendly message from the RFC 7807 payload.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const notifier = inject(NotificationService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      const canRefresh =
        error.status === 401 && !req.context.get(SKIP_AUTH) && auth.refreshToken() !== null;

      if (canRefresh) {
        return handle401(req, next, auth, notifier);
      }

      notify(error, notifier, auth);
      return throwError(() => toProblem(error));
    }),
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthStore,
  notifier: NotificationService,
): Observable<HttpEvent<unknown>> {
  if (isRefreshing) {
    // Wait for the in-flight refresh, then replay this request with the new token.
    return refreshedToken$.pipe(
      filter((t): t is string => t !== null),
      take(1),
      switchMap((token) => next(retryWithToken(req, token))),
    );
  }

  isRefreshing = true;
  refreshedToken$.next(null);

  return auth.refresh().pipe(
    switchMap((token) => {
      isRefreshing = false;
      refreshedToken$.next(token);
      return next(retryWithToken(req, token));
    }),
    catchError((err) => {
      isRefreshing = false;
      notifier.error('Tu sesión expiró. Inicia sesión nuevamente.');
      auth.logout();
      return throwError(() => err);
    }),
  );
}

function retryWithToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function toProblem(error: HttpErrorResponse): ProblemDetail {
  if (isProblemDetail(error.error)) {
    return error.error;
  }
  return {
    type: 'about:blank',
    title: error.statusText || 'Error',
    status: error.status,
    detail: typeof error.error === 'string' ? error.error : error.message,
  };
}

function notify(error: HttpErrorResponse, notifier: NotificationService, auth: AuthStore): void {
  if (error.status === 0) {
    notifier.error('No se pudo conectar con el servidor.');
    return;
  }
  if (error.status === 401) {
    auth.logout();
    return;
  }
  if (error.status === 403) {
    notifier.error('No tienes permisos para esta acción.');
    return;
  }
  // Validation (400) is usually rendered inline by the form; skip the toast.
  if (error.status === 400) {
    return;
  }
  const problem = toProblem(error);
  notifier.error(problem.detail || problem.title || 'Ocurrió un error inesperado.');
}

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { retry, timer } from 'rxjs';

/**
 * Retries idempotent GET requests on transient failures (network errors or 5xx)
 * with exponential backoff. Never retries 4xx or mutating verbs.
 */
export const retryInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') {
    return next(req);
  }
  return next(req).pipe(
    retry({
      count: 2,
      delay: (error: HttpErrorResponse, attempt) => {
        const transient = error.status === 0 || error.status >= 500;
        if (!transient) {
          throw error;
        }
        return timer(attempt * 500);
      },
    }),
  );
};

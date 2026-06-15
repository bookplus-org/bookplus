import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from './auth.store';
import { SKIP_AUTH } from './auth.service';

/**
 * Attaches `Authorization: Bearer <token>` to outgoing requests, unless the
 * request opted out via the SKIP_AUTH context token (login/register/refresh).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(SKIP_AUTH)) {
    return next(req);
  }
  const token = inject(AuthStore).accessToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

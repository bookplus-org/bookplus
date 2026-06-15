import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthStore } from './auth.store';

/** Allows navigation only for authenticated users; otherwise redirects to login. */
export const authGuard: CanMatchFn = (_route, segments): boolean | UrlTree => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  const redirect = '/' + segments.map((s) => s.path).join('/');
  return router.createUrlTree(['/auth/login'], { queryParams: { redirect } });
};

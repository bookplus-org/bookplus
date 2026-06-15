import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthStore } from './auth.store';

/** Keeps already-authenticated users out of the auth (login/register) area. */
export const guestGuard: CanMatchFn = (): boolean | UrlTree => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.isAuthenticated() ? router.createUrlTree(['/catalog']) : true;
};

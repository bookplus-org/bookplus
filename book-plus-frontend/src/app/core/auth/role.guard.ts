import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthStore } from './auth.store';
import { Role } from './auth.models';

/**
 * Factory guard that requires a specific role.
 * Usage: `canMatch: [authGuard, roleGuard('ADMIN')]`.
 */
/** Jerarquía de roles: SUPERADMIN satisface ADMIN, que satisface USER. */
const ROLE_RANK: Record<Role, number> = { USER: 0, REPARTIDOR: 1, ADMIN: 2, SUPERADMIN: 3 };

export function roleGuard(required: Role): CanMatchFn {
  return (): boolean | UrlTree => {
    const auth = inject(AuthStore);
    const router = inject(Router);
    const ok = auth.roles().some((r) => ROLE_RANK[r] >= ROLE_RANK[required]);
    return ok ? true : router.createUrlTree(['/catalog']);
  };
}

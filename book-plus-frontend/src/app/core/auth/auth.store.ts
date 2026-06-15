import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap, map } from 'rxjs';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import {
  AuthResponse,
  AuthSession,
  LoginRequest,
  RegisterRequest,
  Role,
  normalizeRoles,
} from './auth.models';
import { isTokenExpired } from './jwt.util';

/**
 * Single source of truth for authentication state, exposed as signals.
 * Components read `user()`, `isAuthenticated()`, `isAdmin()`; interceptors read
 * `accessToken()`. All mutations funnel through this store so persistence and
 * derived state stay consistent.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly authService = inject(AuthService);
  private readonly storage = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly session = signal<AuthSession | null>(null);

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly roles = computed<Role[]>(() => this.session()?.user.roles ?? []);
  readonly isAdmin = computed(() => {
    const r = this.roles();
    return r.includes('ADMIN') || r.includes('SUPERADMIN');
  });
  readonly isCourier = computed(() => this.roles().includes('REPARTIDOR'));
  readonly emailVerified = computed(() => this.session()?.user.emailVerified ?? true);

  accessToken(): string | null {
    return this.session()?.accessToken ?? null;
  }

  /** Reflects a self-service profile change in the local session. */
  patchUser(partial: { username?: string; email?: string; emailVerified?: boolean }): void {
    const current = this.session();
    if (!current) {
      return;
    }
    const updated: AuthSession = { ...current, user: { ...current.user, ...partial } };
    this.session.set(updated);
    this.storage.write(updated);
  }

  refreshToken(): string | null {
    return this.session()?.refreshToken ?? null;
  }

  /** Called by APP_INITIALIZER: rehydrate a non-expired session from storage. */
  restoreSession(): void {
    const stored = this.storage.read();
    if (stored && !isTokenExpired(stored.accessToken)) {
      this.session.set(stored);
    } else if (stored) {
      this.storage.clear();
    }
  }

  login(payload: LoginRequest): Observable<AuthSession> {
    return this.authService.login(payload).pipe(map((res) => this.persist(res)));
  }

  register(payload: RegisterRequest): Observable<AuthSession> {
    return this.authService.register(payload).pipe(map((res) => this.persist(res)));
  }

  /** Used by the error interceptor to silently renew an expired access token. */
  refresh(): Observable<string> {
    const token = this.refreshToken();
    if (!token) {
      throw new Error('No refresh token available');
    }
    return this.authService.refresh({ refreshToken: token }).pipe(
      map((res) => this.persist(res)),
      map((s) => s.accessToken),
    );
  }

  logout(redirect = true): void {
    this.session.set(null);
    this.storage.clear();
    if (redirect) {
      void this.router.navigate(['/auth/login']);
    }
  }

  private persist(res: AuthResponse): AuthSession {
    const session: AuthSession = {
      accessToken: res.accessToken,
      refreshToken: res.refreshToken,
      user: {
        id: res.userId,
        username: res.username,
        email: res.email,
        roles: normalizeRoles(res.roles),
        emailVerified: res.emailVerified ?? false,
      },
    };
    this.session.set(session);
    this.storage.write(session);
    return session;
  }
}

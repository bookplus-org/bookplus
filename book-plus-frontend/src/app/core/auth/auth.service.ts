import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from './auth.models';

/**
 * HttpContext flag that tells the auth interceptor NOT to attach a bearer token
 * (used by login/register/refresh, which must stay anonymous).
 */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);
const skipAuth = () => new HttpContext().set(SKIP_AUTH, true);

/** Thin HTTP gateway for auth-service endpoints. State lives in AuthStore. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, payload, { context: skipAuth() });
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, payload, { context: skipAuth() });
  }

  refresh(payload: RefreshRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/refresh`, payload, { context: skipAuth() });
  }

  forgotPassword(payload: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/forgot-password`, payload, { context: skipAuth() });
  }

  resetPassword(payload: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/reset-password`, payload, { context: skipAuth() });
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/verify-email`, { token }, { context: skipAuth() });
  }

  resendVerification(): Observable<void> {
    return this.http.post<void>(`${this.base}/me/resend-verification`, {});
  }
}

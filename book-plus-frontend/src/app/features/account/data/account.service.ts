import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface MeResponse {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

/** Self-service account endpoints (auth-service `/api/v1/auth/me`). */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/auth/me`;

  updateProfile(payload: { username: string; email: string }): Observable<MeResponse> {
    return this.http.patch<MeResponse>(`${this.base}/profile`, payload);
  }

  changePassword(payload: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.patch<void>(`${this.base}/password`, payload);
  }
}

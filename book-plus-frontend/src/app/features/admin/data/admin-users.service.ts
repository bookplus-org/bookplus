import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  roles: string[];
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
}

/** Admin-only user management against auth-service (`/api/v1/admin/users`). */
@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin/users`;

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(this.base);
  }

  create(payload: { username: string; email: string; password: string; role: string }): Observable<AdminUser> {
    return this.http.post<AdminUser>(this.base, payload);
  }

  updateProfile(id: string, payload: { username: string; email: string }): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.base}/${id}/profile`, payload);
  }

  resetPassword(id: string, password: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/password`, { password });
  }

  setStatus(id: string, enabled: boolean, reason?: string): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.base}/${id}/status`, { enabled, reason });
  }

  changeRole(id: string, role: string, grant: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.base}/${id}/roles`, { role, grant });
  }
}

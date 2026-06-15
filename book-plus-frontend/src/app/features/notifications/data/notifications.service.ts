import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Page, PageQuery } from '@core/models/page.model';

export interface UserNotification {
  id: string;
  type: string;
  channel: string;
  subject: string;
  status: string;
  referenceId?: string;
  createdAt: string;
  sentAt?: string;
}

/** HTTP gateway for notification-service (`/api/v1/notifications`). */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/notifications`;

  list(query: PageQuery = {}): Observable<Page<UserNotification>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<UserNotification>>(this.base, { params });
  }
}

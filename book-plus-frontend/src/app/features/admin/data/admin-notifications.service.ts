import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AdminNotification {
  id: string;
  type: string;
  channel: string;
  subject: string;
  status: string;
  recipientEmail?: string | null;
  referenceId?: string | null;
  createdAt: string;
  sentAt?: string | null;
}

export interface PagedNotifications {
  content: AdminNotification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Admin view of all sent notifications (notification-service `/api/v1/notifications/admin`). */
@Injectable({ providedIn: 'root' })
export class AdminNotificationsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/notifications/admin`;

  list(page = 0, size = 20): Observable<PagedNotifications> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedNotifications>(this.base, { params });
  }
}

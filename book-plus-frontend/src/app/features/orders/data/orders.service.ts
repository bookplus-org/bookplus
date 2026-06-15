import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Page, PageQuery } from '@core/models/page.model';
import { Order } from '../models/order.model';

/** HTTP gateway for order-service (`/api/v1/orders`). Returns raw responses. */
@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/orders`;

  list(query: PageQuery = {}): Observable<Page<Order>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<Order>>(this.base, { params });
  }

  get(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.base}/${orderId}`);
  }

  /** Cancellation is a DELETE with a reason body on order-service. */
  cancel(orderId: string, reason: string): Observable<Order> {
    return this.http.delete<Order>(`${this.base}/${orderId}`, { body: { reason } });
  }

  /** Customer confirms they received the order. */
  confirmReceipt(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/${orderId}/confirm-receipt`, {});
  }

  /** Customer opens a claim/dispute. */
  openClaim(orderId: string, reason: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/${orderId}/claim`, { reason });
  }

  /** Proof-of-delivery photo as a blob (auth-protected; owner or admin/courier). */
  proofPhoto(orderId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${orderId}/proof/photo`, { responseType: 'blob' });
  }

  /** Proof-of-delivery signature as a blob. */
  proofSignature(orderId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${orderId}/proof/signature`, { responseType: 'blob' });
  }
}

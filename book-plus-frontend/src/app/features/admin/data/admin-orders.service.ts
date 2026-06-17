import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Page } from '@core/models/page.model';
import { Order, OrderStatus } from '@features/orders/models/order.model';

/** Admin-only order management against order-service (`/api/v1/orders/admin`). */
@Injectable({ providedIn: 'root' })
export class AdminOrdersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/orders`;

  listAll(status: OrderStatus | '' , page = 0, size = 20): Observable<Page<Order>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<Order>>(`${this.base}/admin`, { params });
  }

  shipmentsQueue(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/admin/shipments`);
  }

  /** A courier claims an unassigned delivery. */
  claimDelivery(orderId: string): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/${orderId}/claim-delivery`, {});
  }

  ship(orderId: string, payload: { carrier: string; trackingNumber: string }): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/${orderId}/ship`, payload);
  }

  deliver(orderId: string, payload: { deliveryCode: string; receivedBy: string }): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/${orderId}/deliver`, payload);
  }

  /** Marca entregado con prueba: foto (obligatoria) + firma (opcional). */
  deliverWithProof(
    orderId: string,
    payload: { deliveryCode: string; receivedBy: string; photo: File; signature: Blob | null },
  ): Observable<Order> {
    const fd = new FormData();
    fd.append('deliveryCode', payload.deliveryCode);
    fd.append('receivedBy', payload.receivedBy ?? '');
    fd.append('photo', payload.photo, payload.photo.name || 'foto.jpg');
    if (payload.signature) {
      fd.append('signature', payload.signature, 'firma.png');
    }
    return this.http.post<Order>(`${this.base}/${orderId}/deliver-proof`, fd);
  }

  cancel(orderId: string, reason: string): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/${orderId}/cancel`, { reason });
  }

  resolveClaim(orderId: string, resolution: string): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/${orderId}/claim/resolve`, { resolution });
  }

  refund(orderId: string, payload: RefundPayload): Observable<RefundOutcome> {
    return this.http.patch<RefundOutcome>(`${this.base}/${orderId}/refund`, payload);
  }

  /** Hechos de consumo de un libro digital (entrada para la política de reembolsos). */
  consumption(userId: string, bookId: string): Observable<PurchaseConsumption> {
    const params = new HttpParams().set('userId', userId).set('bookId', bookId);
    return this.http.get<PurchaseConsumption>(
      `${environment.apiBaseUrl}/library/admin/consumption`, { params });
  }
}

export interface RefundPayload {
  reason: string;
  restock: boolean;
  downloaded?: boolean;
  readProgress?: number;
  adminOverride?: boolean;
}

export interface RefundOutcome {
  order: Order;
  outcome: 'CASH' | 'STORE_CREDIT' | 'DENY';
  storeCreditCode: string | null;
  policyReason: string;
}

export interface PurchaseConsumption {
  downloaded: boolean;
  readProgress: number;
  active: boolean;
}

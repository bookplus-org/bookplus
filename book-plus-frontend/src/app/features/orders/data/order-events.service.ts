import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AuthStore } from '@core/auth/auth.store';

export interface OrderUpdate {
  orderId: string;
  status: string;
}

/**
 * Cliente SSE de cambios de pedido. Abre un EventSource al stream del order-service
 * (con el JWT como query param, ya que EventSource no envía headers) y emite cada vez
 * que llega un evento `order-update`. El navegador reconecta solo si la conexión cae.
 *
 * Pensado para complementar el polling: da refresco instantáneo cuando hay conexión.
 */
@Injectable({ providedIn: 'root' })
export class OrderEventsService {
  private readonly auth = inject(AuthStore);

  stream(): Observable<OrderUpdate> {
    return new Observable<OrderUpdate>((subscriber) => {
      const token = this.auth.accessToken();
      if (!token) {
        return; // sin sesión no hay stream
      }
      const url = `${environment.apiBaseUrl}/orders/stream?token=${encodeURIComponent(token)}`;
      const source = new EventSource(url);

      source.addEventListener('order-update', (event: MessageEvent) => {
        try {
          subscriber.next(JSON.parse(event.data) as OrderUpdate);
        } catch {
          subscriber.next({ orderId: '', status: '' });
        }
      });

      // No propagamos onerror: EventSource reintenta la conexión automáticamente.
      return () => source.close();
    });
  }
}

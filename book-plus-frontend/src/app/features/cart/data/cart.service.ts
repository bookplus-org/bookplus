import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  AddItemRequest,
  Cart,
  CheckoutRequest,
  CouponValidation,
  UpdateQuantityRequest,
} from '../models/cart.model';

/** HTTP gateway for cart-service (`/api/v1/cart`). Returns raw CartResponse. */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/cart`;

  get(): Observable<Cart> {
    return this.http.get<Cart>(this.base);
  }

  addItem(payload: AddItemRequest): Observable<Cart> {
    return this.http.post<Cart>(`${this.base}/items`, payload);
  }

  updateQuantity(bookId: string, payload: UpdateQuantityRequest): Observable<Cart> {
    return this.http.put<Cart>(`${this.base}/items/${bookId}`, payload);
  }

  removeItem(bookId: string): Observable<Cart> {
    return this.http.delete<Cart>(`${this.base}/items/${bookId}`);
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.base);
  }

  /** Triggers checkout with shipping address + payment method. Order is created async via Kafka. */
  checkout(payload: CheckoutRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/checkout`, payload);
  }

  /** Validates a coupon against an amount (order-service). */
  validateCoupon(code: string, amount: number): Observable<CouponValidation> {
    return this.http.post<CouponValidation>(`${environment.apiBaseUrl}/orders/validate-coupon`, {
      code,
      amount,
    });
  }
}

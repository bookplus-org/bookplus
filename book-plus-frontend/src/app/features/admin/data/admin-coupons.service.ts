import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface Coupon {
  code: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  minAmount?: number | null;
  active: boolean;
  expiresAt?: string | null;
  createdAt?: string | null;
}

export interface CreateCouponPayload {
  code: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  minAmount?: number | null;
  expiresAt?: string | null;
}

/** Admin coupon management against order-service (`/api/v1/orders/admin/coupons`). */
@Injectable({ providedIn: 'root' })
export class AdminCouponsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/orders/admin/coupons`;

  list(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(this.base);
  }

  create(payload: CreateCouponPayload): Observable<Coupon> {
    return this.http.post<Coupon>(this.base, payload);
  }

  setActive(code: string, value: boolean): Observable<Coupon> {
    return this.http.patch<Coupon>(`${this.base}/${encodeURIComponent(code)}/active?value=${value}`, {});
  }

  remove(code: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${encodeURIComponent(code)}`);
  }
}

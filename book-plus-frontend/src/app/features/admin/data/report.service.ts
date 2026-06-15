import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface SalesSummary {
  totalOrders: number;
  totalItemsSold: number;
  totalRevenue: number;
  currency: string;
  totalCancellations: number;
  totalRefunded: number;
}

export interface SalesMetric {
  date: string;
  ordersCount: number;
  itemsSold: number;
  revenue: number;
  currency: string;
  cancellations: number;
  refunds: number;
  refundedAmount: number;
}

export interface TopBook {
  bookId: string;
  isbn: string;
  title: string;
  unitsSold: number;
  revenue: number;
}

/** HTTP gateway for report-service (`/api/v1/reports`). Returns raw responses. */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/reports`;

  private range(from: string, to: string): HttpParams {
    return new HttpParams().set('from', from).set('to', to);
  }

  summary(from: string, to: string): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.base}/sales/summary`, { params: this.range(from, to) });
  }

  daily(from: string, to: string): Observable<SalesMetric[]> {
    return this.http.get<SalesMetric[]>(`${this.base}/sales/daily`, { params: this.range(from, to) });
  }

  topBooks(from: string, to: string, limit = 10): Observable<TopBook[]> {
    return this.http.get<TopBook[]>(`${this.base}/sales/top-books`, {
      params: this.range(from, to).set('limit', limit),
    });
  }

  exportCsv(from: string, to: string): Observable<Blob> {
    return this.http.get(`${this.base}/sales/export/csv`, {
      params: this.range(from, to),
      responseType: 'blob',
    });
  }

  exportPdf(from: string, to: string): Observable<Blob> {
    return this.http.get(`${this.base}/sales/export/pdf`, {
      params: this.range(from, to),
      responseType: 'blob',
    });
  }
}

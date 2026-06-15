import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AdjustStockRequest, Stock } from '../models/inventory.model';

/** Admin operations against inventory-service (`/api/v1/inventory`). */
@Injectable({ providedIn: 'root' })
export class AdminInventoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/inventory`;

  getStock(bookId: string): Observable<Stock> {
    return this.http.get<Stock>(`${this.base}/${bookId}`);
  }

  adjust(bookId: string, payload: AdjustStockRequest): Observable<Stock> {
    return this.http.put<Stock>(`${this.base}/${bookId}/adjust`, payload);
  }
}

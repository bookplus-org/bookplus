import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { BookSummary } from '@features/catalog/models/book.model';

/** Wishlist endpoints (catalog-service `/api/v1/favorites`). */
@Injectable({ providedIn: 'root' })
export class FavoritesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/favorites`;

  list(): Observable<BookSummary[]> {
    return this.http.get<BookSummary[]>(this.base);
  }

  ids(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/ids`);
  }

  add(bookId: string): Observable<void> {
    return this.http.put<void>(`${this.base}/${bookId}`, {});
  }

  remove(bookId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${bookId}`);
  }
}

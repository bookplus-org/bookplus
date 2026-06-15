import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { BookSummary } from '@features/catalog/models/book.model';

/** Purchased books (catalog-service `/api/v1/library`). */
@Injectable({ providedIn: 'root' })
export class LibraryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/library`;

  list(): Observable<BookSummary[]> {
    return this.http.get<BookSummary[]>(this.base);
  }

  download(bookId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${bookId}/book.pdf`, { responseType: 'blob' });
  }
}

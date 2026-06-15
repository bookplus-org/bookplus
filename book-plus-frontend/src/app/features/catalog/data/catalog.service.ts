import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Page } from '@core/models/page.model';
import { Book, BookBrowseParams, BookSummary, Category, Review } from '../models/book.model';

export interface AddReviewRequest {
  rating: number;
  comment: string;
  verifiedPurchase: boolean;
}

/** HTTP gateway for catalog-service (`/api/v1/books`, `/api/v1/categories`). */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly booksUrl = `${environment.apiBaseUrl}/books`;
  private readonly categoriesUrl = `${environment.apiBaseUrl}/categories`;

  /**
   * Browses the catalog. When `q` is present it uses the Elasticsearch-backed
   * `/books/search`; otherwise it lists with optional category/author filters.
   */
  browse(params: BookBrowseParams): Observable<Page<BookSummary>> {
    const { q, categoryId, author, page = 0, size = 12 } = params;
    if (q && q.trim()) {
      const httpParams = new HttpParams()
        .set('q', q.trim())
        .set('page', page)
        .set('size', size);
      return this.http.get<Page<BookSummary>>(`${this.booksUrl}/search`, { params: httpParams });
    }
    let httpParams = new HttpParams().set('page', page).set('size', size);
    if (categoryId) {
      httpParams = httpParams.set('categoryId', categoryId);
    }
    if (author) {
      httpParams = httpParams.set('author', author);
    }
    return this.http.get<Page<BookSummary>>(this.booksUrl, { params: httpParams });
  }

  getBook(id: string): Observable<Book> {
    return this.http.get<Book>(`${this.booksUrl}/${id}`);
  }

  /** Public PDF sample (first pages only). 204 if the book has no uploaded preview. */
  getPreviewPdf(id: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.booksUrl}/${id}/preview.pdf`, {
      responseType: 'blob',
      observe: 'response',
    });
  }

  /** Full PDF for owners (purchased) or admins. 403 if neither, 204 if none uploaded. */
  getFullPdf(id: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${environment.apiBaseUrl}/library/${id}/book.pdf`, {
      responseType: 'blob',
      observe: 'response',
    });
  }

  getReviews(bookId: string, page = 0, size = 10): Observable<Page<Review>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Review>>(`${this.booksUrl}/${bookId}/reviews`, { params });
  }

  addReview(bookId: string, payload: AddReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${this.booksUrl}/${bookId}/reviews`, payload);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }
}

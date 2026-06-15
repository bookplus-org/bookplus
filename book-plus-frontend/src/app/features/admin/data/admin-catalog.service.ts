import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Book, Category } from '@features/catalog/models/book.model';
import {
  CreateBookRequest,
  CreateCategoryRequest,
  UpdateBookRequest,
} from '../models/admin-catalog.model';

/**
 * Admin-only write operations against catalog-service. Reads reuse the public
 * CatalogService; these endpoints require an ADMIN token (enforced at the gateway).
 */
@Injectable({ providedIn: 'root' })
export class AdminCatalogService {
  private readonly http = inject(HttpClient);
  // Admin write endpoints live under /admin/** (catalog-service via the gateway).
  private readonly booksUrl = `${environment.apiBaseUrl}/admin/books`;
  private readonly categoriesUrl = `${environment.apiBaseUrl}/admin/categories`;

  createBook(payload: CreateBookRequest): Observable<Book> {
    return this.http.post<Book>(this.booksUrl, payload);
  }

  updateBook(id: string, payload: UpdateBookRequest): Observable<Book> {
    return this.http.put<Book>(`${this.booksUrl}/${id}`, payload);
  }

  deleteBook(id: string): Observable<void> {
    return this.http.delete<void>(`${this.booksUrl}/${id}`);
  }

  /** Uploads a book PDF; backend keeps only the first pages as the sample. */
  uploadPreview(id: string, file: File): Observable<{ bookId: string; previewPages: number }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ bookId: string; previewPages: number }>(
      `${this.booksUrl}/${id}/preview`,
      form,
    );
  }

  /** Admin-only: full uploaded PDF as a blob (204 → empty if none). */
  getFullPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.booksUrl}/${id}/full.pdf`, { responseType: 'blob' });
  }

  /** Uploads a cover image; backend stores it and points image_url to the cover endpoint. */
  uploadCover(id: string, file: File): Observable<{ bookId: string; imageUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ bookId: string; imageUrl: string }>(
      `${this.booksUrl}/${id}/cover`,
      form,
    );
  }

  createCategory(payload: CreateCategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.categoriesUrl, payload);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.categoriesUrl}/${id}`);
  }
}

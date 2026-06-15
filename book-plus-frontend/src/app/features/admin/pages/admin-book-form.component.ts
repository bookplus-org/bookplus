import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { CatalogService } from '@features/catalog/data/catalog.service';
import { AdminCatalogService } from '../data/admin-catalog.service';
import { Category } from '@features/catalog/models/book.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';
import { applyServerErrors } from '@shared/forms/apply-server-errors';

@Component({
  selector: 'bp-admin-book-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-book-form.component.html',
})
export class AdminBookFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly adminCatalog = inject(AdminCatalogService);
  private readonly notifier = inject(NotificationService);
  private readonly router = inject(Router);

  /** Present on the edit route (`/admin/books/:id/edit`). */
  readonly id = input<string>();

  protected readonly categories = signal<Category[]>([]);
  protected readonly submitting = signal(false);
  protected readonly isEdit = signal(false);
  protected readonly uploadingPdf = signal(false);
  protected readonly previewPages = signal<number | null>(null);
  protected readonly uploadingCover = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    isbn: ['', [Validators.required]],
    title: ['', [Validators.required]],
    author: ['', [Validators.required]],
    description: ['', [Validators.required]],
    price: [0, [Validators.required, Validators.min(0.01)]],
    currency: ['USD', [Validators.required]],
    imageUrl: [''],
    previewUrl: [''],
    publisher: [''],
    publishedDate: this.fb.control<string | null>(null),
    language: ['es'],
    pages: this.fb.control<number | null>(null),
    categoryId: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.catalog.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.categories.set([]),
    });

    const bookId = this.id();
    if (bookId) {
      this.isEdit.set(true);
      this.catalog.getBook(bookId).subscribe({
        next: (book) =>
          this.form.patchValue({
            isbn: book.isbn,
            title: book.title,
            author: book.author,
            description: book.description,
            price: book.price,
            currency: book.currency,
            imageUrl: book.imageUrl ?? '',
            previewUrl: book.previewUrl ?? '',
            publisher: book.publisher ?? '',
            publishedDate: book.publishedDate ?? null,
            language: book.language ?? 'es',
            pages: book.pages ?? null,
            categoryId: book.categoryId,
          }),
        error: () => this.notifier.error('No se pudo cargar el libro.'),
      });
    }
  }

  onCoverSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const bookId = this.id();
    if (!file || !bookId) {
      return;
    }
    if (file.type && !file.type.toLowerCase().startsWith('image/')) {
      this.notifier.error('El archivo debe ser una imagen.');
      input.value = '';
      return;
    }
    this.uploadingCover.set(true);
    this.adminCatalog.uploadCover(bookId, file).subscribe({
      next: (res) => {
        this.uploadingCover.set(false);
        // Cache-bust so the new image shows immediately.
        this.form.patchValue({ imageUrl: `${res.imageUrl}?t=${Date.now()}` });
        this.notifier.success('Portada actualizada.');
        input.value = '';
      },
      error: (problem: ProblemDetail) => {
        this.uploadingCover.set(false);
        this.notifier.error(problem.detail ?? 'No se pudo subir la portada.');
        input.value = '';
      },
    });
  }

  viewFullPdf(): void {
    const bookId = this.id();
    if (!bookId) {
      return;
    }
    // Abre nuestro visor (overlay) en modo "libro completo".
    void this.router.navigate(['/catalog', bookId, 'preview'], { queryParams: { full: 1 } });
  }

  onPdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const bookId = this.id();
    if (!file || !bookId) {
      return;
    }
    if (file.type && !file.type.toLowerCase().includes('pdf')) {
      this.notifier.error('El archivo debe ser un PDF.');
      input.value = '';
      return;
    }
    this.uploadingPdf.set(true);
    this.adminCatalog.uploadPreview(bookId, file).subscribe({
      next: (res) => {
        this.uploadingPdf.set(false);
        this.previewPages.set(res.previewPages);
        this.notifier.success(`Vista previa generada (${res.previewPages} páginas).`);
        input.value = '';
      },
      error: (problem: ProblemDetail) => {
        this.uploadingPdf.set(false);
        this.notifier.error(problem.detail ?? 'No se pudo subir el PDF.');
        input.value = '';
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    // Empty date string → null so the backend doesn't try to parse "" as a LocalDate.
    const value = { ...raw, publishedDate: raw.publishedDate || null };
    const bookId = this.id();
    const request$ = bookId
      ? this.adminCatalog.updateBook(bookId, value)
      : this.adminCatalog.createBook(value);

    request$.subscribe({
      next: () => {
        this.notifier.success(bookId ? 'Libro actualizado.' : 'Libro creado.');
        void this.router.navigate(['/admin/books']);
      },
      error: (problem: ProblemDetail) => {
        this.submitting.set(false);
        if (!applyServerErrors(this.form, problem)) {
          this.notifier.error(problem.detail ?? 'No se pudo guardar el libro.');
        }
      },
    });
  }
}

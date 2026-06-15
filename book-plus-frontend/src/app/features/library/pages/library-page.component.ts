import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LibraryService } from '../data/library.service';
import { BookSummary } from '@features/catalog/models/book.model';
import { NotificationService } from '@core/notifications/notification.service';

@Component({
  selector: 'bp-library-page',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="bp-section-title mb-1">Mi biblioteca</h1>
    <p class="mb-6 text-sm text-ink-500">Tus libros comprados. Léelos o descárgalos las veces que quieras.</p>

    @if (loading()) {
      <div class="flex justify-center py-24"><mat-spinner diameter="48" /></div>
    } @else if (books().length === 0) {
      <div class="flex flex-col items-center gap-3 py-24 text-center">
        <mat-icon fontSet="material-symbols-outlined" class="!text-6xl text-ink-200">auto_stories</mat-icon>
        <p class="text-ink-500">Aún no has comprado libros.</p>
        <a mat-flat-button color="primary" routerLink="/catalog">Explorar catálogo</a>
      </div>
    } @else {
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        @for (book of books(); track book.id) {
          <div class="bp-card flex gap-4 p-4">
            <div class="flex h-28 w-20 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-slate-100">
              @if (book.imageUrl) {
                <img [src]="book.imageUrl" [alt]="book.title" class="h-full w-full object-cover" />
              } @else {
                <mat-icon fontSet="material-symbols-outlined" class="!text-4xl text-brand-300">menu_book</mat-icon>
              }
            </div>
            <div class="flex min-w-0 flex-1 flex-col">
              <a [routerLink]="['/catalog', book.id]" class="line-clamp-2 font-semibold text-ink-900 no-underline hover:text-brand-600">
                {{ book.title }}
              </a>
              <span class="text-sm text-ink-500">{{ book.author }}</span>
              <div class="mt-auto flex gap-2 pt-2">
                <a mat-flat-button color="primary" class="!rounded-full" [routerLink]="['/catalog', book.id, 'preview']" [queryParams]="{ full: 1 }">
                  <mat-icon fontSet="material-symbols-outlined">menu_book</mat-icon>
                  Leer
                </a>
                <button mat-stroked-button class="!rounded-full" [disabled]="downloadingId() === book.id" (click)="download(book)">
                  <mat-icon fontSet="material-symbols-outlined">download</mat-icon>
                  Descargar
                </button>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `,
})
export class LibraryPageComponent implements OnInit {
  private readonly service = inject(LibraryService);
  private readonly notifier = inject(NotificationService);

  protected readonly books = signal<BookSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly downloadingId = signal<string | null>(null);

  ngOnInit(): void {
    this.service.list().subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  download(book: BookSummary): void {
    this.downloadingId.set(book.id);
    this.service.download(book.id).subscribe({
      next: (blob) => {
        this.downloadingId.set(null);
        if (!blob || blob.size === 0) {
          this.notifier.error('Este libro aún no tiene PDF disponible.');
          return;
        }
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${book.title}.pdf`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: () => {
        this.downloadingId.set(null);
        this.notifier.error('No se pudo descargar el libro.');
      },
    });
  }
}

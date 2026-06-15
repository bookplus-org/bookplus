import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../data/catalog.service';
import { Book } from '../models/book.model';

interface PreviewVm {
  book: Book;
  pdfUrl: SafeResourceUrl | null;
  previewPages: number | null;
  totalPages: number | null;
  isFull: boolean;
}

type State =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; vm: PreviewVm };

/**
 * Visor de muestra del libro: renderiza el PDF de muestra que sirve el backend
 * (solo las primeras páginas). El resto queda bloqueado hasta la compra.
 * Se presenta como overlay: clic fuera o Escape lo cierra.
 */
@Component({
  selector: 'bp-book-preview-page',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './book-preview-page.component.html',
})
export class BookPreviewPageComponent {
  private readonly catalog = inject(CatalogService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();
  /** Query param `full=1` → carga el PDF completo (solo-admin). */
  readonly full = input<string>();
  protected readonly state = signal<State>({ status: 'loading' });

  private objectUrl: string | null = null;

  constructor() {
    effect(() => this.load(this.id(), !!this.full()));
    this.destroyRef.onDestroy(() => this.revoke());
  }

  /** Cierra el visor y vuelve al detalle del libro. */
  @HostListener('document:keydown.escape')
  close(): void {
    void this.router.navigate(['/catalog', this.id()]);
  }

  private load(id: string, full: boolean): void {
    this.state.set({ status: 'loading' });
    this.revoke();

    const pdf$ = full ? this.catalog.getFullPdf(id) : this.catalog.getPreviewPdf(id);

    forkJoin({
      book: this.catalog.getBook(id),
      res: pdf$.pipe(catchError(() => of(null))),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ book, res }) => {
          let pdfUrl: SafeResourceUrl | null = null;
          let previewPages: number | null = null;
          let totalPages: number | null = null;

          const blob = res?.body;
          if (blob && blob.size > 0) {
            this.objectUrl = URL.createObjectURL(blob);
            // Sin #toolbar=0: dejamos la barra del visor para ver "página X / Y".
            pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
            previewPages = Number(res!.headers.get('X-Preview-Pages')) || null;
            const tp = res!.headers.get('X-Total-Pages');
            totalPages = tp ? Number(tp) || null : null;
          }
          this.state.set({ status: 'ready', vm: { book, pdfUrl, previewPages, totalPages, isFull: full } });
        },
        error: () => this.state.set({ status: 'error', error: 'No se encontró el libro.' }),
      });
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}

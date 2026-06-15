import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CatalogService } from '@features/catalog/data/catalog.service';
import { AdminCatalogService } from '../data/admin-catalog.service';
import { BookSummary } from '@features/catalog/models/book.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '@shared/ui/confirm-dialog/confirm-dialog.component';

const PAGE_SIZE = 15;

@Component({
  selector: 'bp-admin-books-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-books-page.component.html',
})
export class AdminBooksPageComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly adminCatalog = inject(AdminCatalogService);
  private readonly notifier = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns = ['title', 'author', 'price', 'actions'];
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly books = signal<BookSummary[]>([]);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(false);
  protected readonly pageSize = PAGE_SIZE;

  ngOnInit(): void {
    this.search.valueChanges
      .pipe(startWith(''), debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex.set(0);
        this.fetch();
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.fetch();
  }

  confirmDelete(book: BookSummary): void {
    const data: ConfirmDialogData = {
      title: 'Eliminar libro',
      message: `¿Eliminar "${book.title}"? Esta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
      danger: true,
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.delete(book);
        }
      });
  }

  private delete(book: BookSummary): void {
    this.adminCatalog.deleteBook(book.id).subscribe({
      next: () => {
        this.notifier.success('Libro eliminado.');
        this.fetch();
      },
      error: (problem: ProblemDetail) =>
        this.notifier.error(problem.detail ?? 'No se pudo eliminar.'),
    });
  }

  private fetch(): void {
    this.loading.set(true);
    this.catalog
      .browse({ q: this.search.value || undefined, page: this.pageIndex(), size: this.pageSize })
      .subscribe({
        next: (page) => {
          this.books.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}

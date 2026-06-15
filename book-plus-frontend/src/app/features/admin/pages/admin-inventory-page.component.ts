import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CatalogService } from '@features/catalog/data/catalog.service';
import { AdminInventoryService } from '../data/admin-inventory.service';
import { BookSummary } from '@features/catalog/models/book.model';
import { Stock } from '../models/inventory.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-admin-inventory-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatListModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-inventory-page.component.html',
})
export class AdminInventoryPageComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly inventory = inject(AdminInventoryService);
  private readonly fb = inject(FormBuilder);
  private readonly notifier = inject(NotificationService);

  private readonly destroyRef = inject(DestroyRef);

  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly books = signal<BookSummary[]>([]);
  protected readonly selected = signal<BookSummary | null>(null);
  protected readonly stock = signal<Stock | null>(null);
  protected readonly loadingStock = signal(false);
  protected readonly saving = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    newTotalQuantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    lowStockThreshold: [5, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  ngOnInit(): void {
    this.search.valueChanges
      .pipe(startWith(''), debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((q) => this.fetchBooks(q));
  }

  select(book: BookSummary): void {
    this.selected.set(book);
    this.stock.set(null);
    this.loadingStock.set(true);
    this.inventory.getStock(book.id).subscribe({
      next: (stock) => {
        this.stock.set(stock);
        this.form.patchValue({
          newTotalQuantity: stock.quantityTotal,
          lowStockThreshold: stock.lowStockThreshold,
          notes: '',
        });
        this.loadingStock.set(false);
      },
      error: () => {
        // No stock record yet — let the admin initialize it via adjust.
        this.stock.set(null);
        this.form.patchValue({ newTotalQuantity: 0, lowStockThreshold: 5, notes: '' });
        this.loadingStock.set(false);
      },
    });
  }

  save(): void {
    const book = this.selected();
    if (!book || this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const { newTotalQuantity, lowStockThreshold, notes } = this.form.getRawValue();
    this.inventory
      .adjust(book.id, { newTotalQuantity: newTotalQuantity ?? 0, lowStockThreshold, notes })
      .subscribe({
        next: (stock) => {
          this.stock.set(stock);
          this.saving.set(false);
          this.notifier.success('Stock actualizado.');
        },
        error: (problem: ProblemDetail) => {
          this.saving.set(false);
          this.notifier.error(problem.detail ?? 'No se pudo ajustar el stock.');
        },
      });
  }

  private fetchBooks(q: string): void {
    this.catalog.browse({ q: q || undefined, size: 10 }).subscribe({
      next: (page) => this.books.set(page.content),
      error: () => this.books.set([]),
    });
  }
}

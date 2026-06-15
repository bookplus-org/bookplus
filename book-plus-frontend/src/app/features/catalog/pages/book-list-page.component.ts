import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CatalogService } from '../data/catalog.service';
import { BookSummary, Category } from '../models/book.model';
import { Page } from '@core/models/page.model';
import { AsyncState, failure, idle, loading, success } from '@core/models/async-state.model';
import { CartStore } from '@features/cart/data/cart.store';
import { FavoritesStore } from '@features/favorites/data/favorites.store';
import { ProblemDetail } from '@core/models/problem-detail.model';
import { BookCardComponent } from '@shared/ui/book-card/book-card.component';
import { BookCardSkeletonComponent } from '@shared/ui/skeleton/book-card-skeleton.component';

const PAGE_SIZE = 12;

@Component({
  selector: 'bp-book-list-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatPaginatorModule,
    BookCardComponent,
    BookCardSkeletonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './book-list-page.component.html',
})
export class BookListPageComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartStore);
  private readonly favorites = inject(FavoritesStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly categoryId = signal<string>('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = PAGE_SIZE;

  protected readonly categories = signal<Category[]>([]);
  protected readonly state = signal<AsyncState<Page<BookSummary>>>(idle());

  protected readonly trust = [
    { icon: 'local_shipping', title: 'Envío rápido', subtitle: 'En 24-48 horas' },
    { icon: 'lock', title: 'Pago seguro', subtitle: 'Cifrado de extremo a extremo' },
    { icon: 'autorenew', title: 'Devoluciones', subtitle: '30 días sin preguntas' },
  ];

  ngOnInit(): void {
    this.favorites.ensureLoaded();

    this.catalog.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.categories.set([]),
    });

    this.search.valueChanges
      .pipe(
        startWith(''),
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.pageIndex.set(0);
        this.fetch();
      });
  }

  onCategoryChange(id: string): void {
    this.categoryId.set(id);
    this.pageIndex.set(0);
    this.fetch();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.fetch();
  }

  addToCart(book: BookSummary): void {
    this.cart.add(book, 1);
  }

  private fetch(): void {
    this.state.set(loading());
    this.catalog
      .browse({
        q: this.search.value || undefined,
        categoryId: this.categoryId() || undefined,
        page: this.pageIndex(),
        size: this.pageSize,
      })
      .subscribe({
        next: (page) => this.state.set(success(page)),
        error: (problem: ProblemDetail) =>
          this.state.set(failure(problem.detail ?? 'No se pudo cargar el catálogo.')),
      });
  }
}

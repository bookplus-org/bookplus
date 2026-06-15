import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BookCardComponent } from '@shared/ui/book-card/book-card.component';
import { BookSummary } from '@features/catalog/models/book.model';
import { FavoritesService } from '../data/favorites.service';
import { FavoritesStore } from '../data/favorites.store';
import { CartStore } from '@features/cart/data/cart.store';

@Component({
  selector: 'bp-favorites-page',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule, BookCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="bp-section-title mb-6">Mis favoritos</h1>

    @if (loading()) {
      <div class="flex justify-center py-24"><mat-spinner diameter="48" /></div>
    } @else if (visible().length === 0) {
      <div class="flex flex-col items-center gap-3 py-24 text-center">
        <mat-icon fontSet="material-symbols-outlined" class="!text-6xl text-ink-200">favorite</mat-icon>
        <p class="text-ink-500">Aún no tienes libros favoritos.</p>
        <a mat-flat-button color="primary" routerLink="/catalog">Explorar catálogo</a>
      </div>
    } @else {
      <div class="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4">
        @for (book of visible(); track book.id) {
          <bp-book-card [book]="book" (addToCart)="cart.add($event, 1)" />
        }
      </div>
    }
  `,
})
export class FavoritesPageComponent implements OnInit {
  private readonly service = inject(FavoritesService);
  private readonly favorites = inject(FavoritesStore);
  protected readonly cart = inject(CartStore);

  protected readonly loading = signal(true);
  private readonly books = signal<BookSummary[]>([]);

  /** Hide a card as soon as it's un-favorited. */
  protected readonly visible = computed(() =>
    this.books().filter((b) => this.favorites.isFavorite(b.id)),
  );

  ngOnInit(): void {
    this.service.list().subscribe({
      next: (books) => {
        this.books.set(books);
        this.favorites.setIds(books.map((b) => b.id));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

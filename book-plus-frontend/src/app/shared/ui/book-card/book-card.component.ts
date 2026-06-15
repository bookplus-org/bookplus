import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BookSummary } from '@features/catalog/models/book.model';
import { FavoritesStore } from '@features/favorites/data/favorites.store';

@Component({
  selector: 'bp-book-card',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bp-card bp-card-hover group flex h-full flex-col overflow-hidden">
      <a [routerLink]="['/catalog', book().id]" class="relative block">
        <div class="flex aspect-[3/4] items-center justify-center overflow-hidden bg-gradient-to-br from-brand-50 to-slate-100">
          @if (book().imageUrl) {
            <img
              [src]="book().imageUrl"
              [alt]="book().title"
              class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              loading="lazy"
            />
          } @else {
            <mat-icon fontSet="material-symbols-outlined" class="!h-16 !w-16 !text-6xl text-brand-300">
              menu_book
            </mat-icon>
          }
        </div>
        @if (discountPct() > 0) {
          <span class="bp-chip absolute left-3 top-3 bg-rose-600 text-white shadow">-{{ discountPct() }}%</span>
        }
        @if (!book().inStock) {
          <span class="bp-chip absolute right-3 top-3 bg-slate-800/80 text-white">Agotado</span>
        }
        <button
          type="button"
          class="absolute bottom-3 right-3 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 shadow ring-1 ring-black/5 transition hover:scale-110"
          (click)="$event.preventDefault(); $event.stopPropagation(); fav.toggle(book().id)"
          [attr.aria-label]="isFav() ? 'Quitar de favoritos' : 'Añadir a favoritos'"
        >
          <mat-icon
            fontSet="material-symbols-outlined"
            class="!text-xl"
            [class.text-rose-600]="isFav()"
            [class.text-ink-400]="!isFav()"
            [class.bp-fav-filled]="isFav()"
          >favorite</mat-icon>
        </button>
      </a>

      <div class="flex flex-1 flex-col p-4">
        <a
          [routerLink]="['/catalog', book().id]"
          class="line-clamp-2 font-semibold leading-snug text-ink-900 no-underline hover:text-brand-600"
        >
          {{ book().title }}
        </a>
        <span class="mt-0.5 text-sm text-ink-500">{{ book().author }}</span>

        @if (book().reviewCount > 0) {
          <span class="mt-1 inline-flex items-center gap-1 text-xs text-amber-500">
            <mat-icon fontSet="material-symbols-outlined" class="!text-sm !leading-none">star</mat-icon>
            {{ book().averageRating | number: '1.1-1' }}
            <span class="text-ink-300">({{ book().reviewCount }})</span>
          </span>
        }

        <div class="mt-auto flex items-end justify-between pt-3">
          <div>
            @if (book().hasDiscount && book().discountPrice) {
              <div class="text-lg font-bold text-ink-900">{{ book().discountPrice | currency: book().currency }}</div>
              <div class="text-xs text-ink-300 line-through">{{ book().price | currency: book().currency }}</div>
            } @else {
              <div class="text-lg font-bold text-ink-900">{{ book().price | currency: book().currency }}</div>
            }
          </div>
          <button
            mat-flat-button
            color="primary"
            class="!min-w-0 !rounded-full !px-3"
            [disabled]="!book().inStock"
            (click)="addToCart.emit(book())"
            [attr.aria-label]="'Añadir ' + book().title"
          >
            <mat-icon fontSet="material-symbols-outlined" class="!m-0">add_shopping_cart</mat-icon>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`.bp-fav-filled { font-variation-settings: 'FILL' 1; }`],
})
export class BookCardComponent {
  protected readonly fav = inject(FavoritesStore);

  readonly book = input.required<BookSummary>();
  readonly addToCart = output<BookSummary>();

  protected readonly isFav = computed(() => this.fav.isFavorite(this.book().id));

  protected readonly discountPct = computed(() => {
    const b = this.book();
    if (b.hasDiscount && b.discountPrice && b.price > 0) {
      return Math.round((1 - b.discountPrice / b.price) * 100);
    }
    return 0;
  });
}

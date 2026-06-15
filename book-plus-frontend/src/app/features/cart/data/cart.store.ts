import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { CartService } from './cart.service';
import { AuthStore } from '@core/auth/auth.store';
import { NotificationService } from '@core/notifications/notification.service';
import { Cart, CartItem, EMPTY_CART } from '../models/cart.model';
import { BookSummary } from '@features/catalog/models/book.model';

/**
 * Signal-based cart state shared between the toolbar badge, the catalog and the
 * cart page. Loads lazily when the user is authenticated and clears on logout.
 */
@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly cartService = inject(CartService);
  private readonly auth = inject(AuthStore);
  private readonly notifier = inject(NotificationService);

  private readonly _cart = signal<Cart>(EMPTY_CART);
  private readonly _loading = signal(false);

  readonly cart = this._cart.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly items = computed<CartItem[]>(() => this._cart().items);
  readonly count = computed(() => this._cart().itemCount);
  readonly subtotal = computed(() => this._cart().total);
  readonly isEmpty = computed(() => this._cart().items.length === 0);

  constructor() {
    // Keep the cart in sync with auth state.
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.load();
      } else {
        this._cart.set(EMPTY_CART);
      }
    });
  }

  load(): void {
    this._loading.set(true);
    this.cartService.get().subscribe({
      next: (cart) => this._cart.set(cart),
      error: () => this._cart.set(EMPTY_CART),
      complete: () => this._loading.set(false),
    });
  }

  /** Adds a catalog book to the cart, sending the denormalized item cart-service needs. */
  add(book: BookSummary, quantity = 1): void {
    if (!this.auth.isAuthenticated()) {
      this.notifier.info('Inicia sesión para agregar al carrito.');
      return;
    }
    this.mutate(
      this.cartService.addItem({
        bookId: book.id,
        isbn: book.isbn,
        title: book.title,
        imageUrl: book.imageUrl,
        unitPrice: book.hasDiscount && book.discountPrice ? book.discountPrice : book.price,
        currency: book.currency,
        quantity,
      }),
      'Añadido al carrito',
    );
  }

  updateQuantity(bookId: string, quantity: number): void {
    if (quantity < 1) {
      this.remove(bookId);
      return;
    }
    this.mutate(this.cartService.updateQuantity(bookId, { quantity }));
  }

  remove(bookId: string): void {
    this.mutate(this.cartService.removeItem(bookId), 'Eliminado del carrito');
  }

  clear(): void {
    this._loading.set(true);
    this.cartService.clear().subscribe({
      next: () => this._cart.set(EMPTY_CART),
      complete: () => this._loading.set(false),
    });
  }

  private mutate(source: Observable<Cart>, successMessage?: string): void {
    this._loading.set(true);
    source.subscribe({
      next: (cart) => {
        this._cart.set(cart);
        if (successMessage) {
          this.notifier.success(successMessage);
        }
      },
      complete: () => this._loading.set(false),
      error: () => this._loading.set(false),
    });
  }
}

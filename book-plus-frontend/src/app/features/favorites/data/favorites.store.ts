import { Injectable, computed, inject, signal } from '@angular/core';
import { FavoritesService } from './favorites.service';
import { AuthStore } from '@core/auth/auth.store';
import { NotificationService } from '@core/notifications/notification.service';

/**
 * In-memory wishlist state. Tracks which book ids are favorited so cards can
 * render the heart and toggle optimistically.
 */
@Injectable({ providedIn: 'root' })
export class FavoritesStore {
  private readonly service = inject(FavoritesService);
  private readonly auth = inject(AuthStore);
  private readonly notifier = inject(NotificationService);

  private readonly _ids = signal<ReadonlySet<string>>(new Set());
  readonly count = computed(() => this._ids().size);

  private loaded = false;

  /** Loads favorite ids once for an authenticated user. */
  ensureLoaded(): void {
    if (this.loaded || !this.auth.isAuthenticated()) {
      return;
    }
    this.loaded = true;
    this.service.ids().subscribe({
      next: (ids) => this._ids.set(new Set(ids)),
      error: () => {
        this.loaded = false;
      },
    });
  }

  isFavorite(bookId: string): boolean {
    return this._ids().has(bookId);
  }

  /** Seeds the set explicitly (e.g., from a loaded favorites list). */
  setIds(ids: string[]): void {
    this._ids.set(new Set(ids));
    this.loaded = true;
  }

  toggle(bookId: string): void {
    if (!this.auth.isAuthenticated()) {
      this.notifier.error('Inicia sesión para guardar favoritos.');
      return;
    }
    const next = new Set(this._ids());
    const wasFav = next.has(bookId);
    if (wasFav) {
      next.delete(bookId);
    } else {
      next.add(bookId);
    }
    this._ids.set(next); // optimistic

    const req = wasFav ? this.service.remove(bookId) : this.service.add(bookId);
    req.subscribe({
      error: () => {
        // revert on failure
        const reverted = new Set(this._ids());
        if (wasFav) {
          reverted.add(bookId);
        } else {
          reverted.delete(bookId);
        }
        this._ids.set(reverted);
        this.notifier.error('No se pudo actualizar favoritos.');
      },
    });
  }

  /** Forces a reload (e.g., after login). */
  reload(): void {
    this.loaded = false;
    this.ensureLoaded();
  }
}

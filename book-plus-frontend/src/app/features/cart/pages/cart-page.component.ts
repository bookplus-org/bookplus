import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { CartStore } from '../data/cart.store';
import { AuthStore } from '@core/auth/auth.store';

@Component({
  selector: 'bp-cart-page',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatIconModule, MatDividerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cart-page.component.html',
})
export class CartPageComponent {
  protected readonly cart = inject(CartStore);
  protected readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  inc(bookId: string, quantity: number): void {
    this.cart.updateQuantity(bookId, quantity + 1);
  }

  dec(bookId: string, quantity: number): void {
    this.cart.updateQuantity(bookId, quantity - 1);
  }

  remove(bookId: string): void {
    this.cart.remove(bookId);
  }

  goToCheckout(): void {
    void this.router.navigate(['/checkout']);
  }
}

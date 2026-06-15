import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthStore } from '@core/auth/auth.store';
import { AuthService } from '@core/auth/auth.service';
import { CartStore } from '@features/cart/data/cart.store';
import { LoadingService } from '@core/http/loading.service';
import { NotificationService } from '@core/notifications/notification.service';

@Component({
  selector: 'bp-store-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatMenuModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './store-layout.component.html',
  styleUrl: './store-layout.component.scss',
})
export class StoreLayoutComponent {
  protected readonly auth = inject(AuthStore);
  protected readonly cart = inject(CartStore);
  protected readonly loading = inject(LoadingService);
  private readonly authService = inject(AuthService);
  private readonly notifier = inject(NotificationService);

  logout(): void {
    this.auth.logout();
  }

  resendVerification(): void {
    this.authService.resendVerification().subscribe({
      next: () => this.notifier.success('Te reenviamos el correo de verificación.'),
      error: () => this.notifier.error('No se pudo reenviar el correo.'),
    });
  }
}

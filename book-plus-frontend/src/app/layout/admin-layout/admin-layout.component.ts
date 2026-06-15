import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthStore } from '@core/auth/auth.store';
import { LoadingService } from '@core/http/loading.service';

@Component({
  selector: 'bp-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading.isLoading()) {
      <mat-progress-bar mode="indeterminate" class="!fixed left-0 top-0 z-50 w-full" />
    }
    <mat-toolbar color="primary" class="sticky top-0 z-10">
      <span class="text-lg font-semibold">BookPlus · Admin</span>
      <span class="flex-1"></span>
      <a mat-button routerLink="/catalog">
        <mat-icon fontSet="material-symbols-outlined">storefront</mat-icon>
        Ver tienda
      </a>
      <button mat-button (click)="auth.logout()">Salir</button>
    </mat-toolbar>

    <mat-sidenav-container class="min-h-[calc(100vh-64px)]">
      <mat-sidenav mode="side" opened class="w-60 border-r">
        <mat-nav-list>
          <a mat-list-item routerLink="/admin/dashboard" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">dashboard</mat-icon>
            Dashboard
          </a>
          <a mat-list-item routerLink="/admin/books" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">library_books</mat-icon>
            Catálogo
          </a>
          <a mat-list-item routerLink="/admin/categories" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">category</mat-icon>
            Categorías
          </a>
          <a mat-list-item routerLink="/admin/inventory" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">inventory_2</mat-icon>
            Inventario
          </a>
          <a mat-list-item routerLink="/admin/orders" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">receipt_long</mat-icon>
            Pedidos
          </a>
          <a mat-list-item routerLink="/admin/shipments" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">local_shipping</mat-icon>
            Envíos
          </a>
          <a mat-list-item routerLink="/admin/users" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">group</mat-icon>
            Usuarios
          </a>
          <a mat-list-item routerLink="/admin/coupons" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">sell</mat-icon>
            Cupones
          </a>
          <a mat-list-item routerLink="/admin/notifications" routerLinkActive="bp-active">
            <mat-icon matListItemIcon fontSet="material-symbols-outlined">mark_email_read</mat-icon>
            Notificaciones
          </a>
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content class="bg-gray-50 p-6">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`.bp-active { font-weight: 600; }`],
})
export class AdminLayoutComponent {
  protected readonly auth = inject(AuthStore);
  protected readonly loading = inject(LoadingService);
}

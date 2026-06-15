import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthStore } from '@core/auth/auth.store';
import { LoadingService } from '@core/http/loading.service';

@Component({
  selector: 'bp-courier-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading.isLoading()) {
      <mat-progress-bar mode="indeterminate" class="!fixed left-0 top-0 z-50 w-full" />
    }
    <mat-toolbar color="primary" class="sticky top-0 z-10">
      <mat-icon fontSet="material-symbols-outlined" class="!mr-2">local_shipping</mat-icon>
      <span class="text-lg font-semibold">BookPlus · Repartidor</span>
      <span class="flex-1"></span>
      @if (auth.isAdmin()) {
        <a mat-button routerLink="/admin">Panel admin</a>
      }
      <button mat-button (click)="auth.logout()">Salir</button>
    </mat-toolbar>

    <main class="mx-auto w-full max-w-5xl px-4 py-8">
      <router-outlet />
    </main>
  `,
})
export class CourierLayoutComponent {
  protected readonly auth = inject(AuthStore);
  protected readonly loading = inject(LoadingService);
}

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoadingService } from '@core/http/loading.service';

@Component({
  selector: 'bp-auth-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading.isLoading()) {
      <mat-progress-bar mode="indeterminate" class="!fixed left-0 top-0 z-50 w-full" />
    }
    <div class="flex min-h-screen items-center justify-center bg-brand-50 px-4 py-10">
      <div class="w-full max-w-md">
        <a routerLink="/catalog" class="mb-8 flex items-center justify-center gap-2 text-brand-600">
          <mat-icon fontSet="material-symbols-outlined">menu_book</mat-icon>
          <span class="text-2xl font-bold">BookPlus</span>
        </a>
        <router-outlet />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {
  protected readonly loading = inject(LoadingService);
}

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '@core/auth/auth.service';
import { AuthStore } from '@core/auth/auth.store';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-verify-email-page',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div class="w-full max-w-md rounded-xl2 border border-slate-200 bg-white p-8 text-center shadow-card">
        @switch (state()) {
          @case ('loading') {
            <mat-spinner diameter="48" class="!mx-auto" />
            <p class="mt-4 text-ink-500">Verificando tu correo…</p>
          }
          @case ('success') {
            <mat-icon fontSet="material-symbols-outlined" class="!text-6xl text-green-600">check_circle</mat-icon>
            <h1 class="mt-3 text-xl font-semibold text-ink-900">¡Correo verificado!</h1>
            <p class="mt-1 text-sm text-ink-500">Tu cuenta ya está activada.</p>
            <a mat-flat-button color="primary" class="!mt-6 !rounded-full" routerLink="/catalog">Ir a la tienda</a>
          }
          @case ('error') {
            <mat-icon fontSet="material-symbols-outlined" class="!text-6xl text-red-500">error</mat-icon>
            <h1 class="mt-3 text-xl font-semibold text-ink-900">No se pudo verificar</h1>
            <p class="mt-1 text-sm text-ink-500">{{ message() }}</p>
            <a mat-stroked-button class="!mt-6 !rounded-full" routerLink="/catalog">Volver a la tienda</a>
          }
        }
      </div>
    </div>
  `,
})
export class VerifyEmailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly auth = inject(AuthStore);

  protected readonly state = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly message = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('error');
      this.message.set('Enlace de verificación inválido.');
      return;
    }
    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.auth.patchUser({ emailVerified: true });
        this.state.set('success');
      },
      error: (problem: ProblemDetail) => {
        this.state.set('error');
        this.message.set(problem.detail ?? 'El enlace no es válido o ha expirado.');
      },
    });
  }
}

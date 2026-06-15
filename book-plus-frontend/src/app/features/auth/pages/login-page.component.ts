import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthStore } from '@core/auth/auth.store';
import { ProblemDetail } from '@core/models/problem-detail.model';
import { applyServerErrors } from '@shared/forms/apply-server-errors';

@Component({
  selector: 'bp-login-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    usernameOrEmail: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const requested = this.route.snapshot.queryParamMap.get('redirect');
        // Sin destino explícito: el repartidor (no admin) va a su vista de entregas.
        const fallback = this.auth.isCourier() && !this.auth.isAdmin() ? '/courier' : '/catalog';
        void this.router.navigateByUrl(requested ?? fallback);
      },
      error: (problem: ProblemDetail) => {
        this.submitting.set(false);
        if (!applyServerErrors(this.form, problem)) {
          this.form.controls.password.setErrors({ server: 'Credenciales inválidas' });
        }
      },
    });
  }
}

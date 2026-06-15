import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  return group.get('newPassword')?.value === group.get('confirmPassword')?.value
    ? null
    : { mismatch: true };
}

@Component({
  selector: 'bp-reset-password-page',
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
  templateUrl: './reset-password-page.component.html',
})
export class ResetPasswordPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notifier = inject(NotificationService);
  private readonly router = inject(Router);

  /** Reset token, bound from `?token=` via withComponentInputBinding(). */
  readonly token = input<string>('');
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.token()) {
      this.notifier.error('Enlace de restablecimiento inválido.');
      return;
    }
    this.submitting.set(true);
    this.authService
      .resetPassword({ token: this.token(), newPassword: this.form.getRawValue().newPassword })
      .subscribe({
        next: () => {
          this.notifier.success('Contraseña actualizada. Ya puedes ingresar.');
          void this.router.navigate(['/auth/login']);
        },
        error: (problem: ProblemDetail) => {
          this.submitting.set(false);
          this.notifier.error(problem.detail ?? 'No se pudo restablecer la contraseña.');
        },
      });
  }
}

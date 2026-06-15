import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthStore } from '@core/auth/auth.store';
import { AccountService } from '../data/account.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-account-page',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-page.component.html',
})
export class AccountPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly account = inject(AccountService);
  private readonly notifier = inject(NotificationService);

  protected readonly auth = inject(AuthStore);
  protected readonly savingProfile = signal(false);
  protected readonly savingPassword = signal(false);

  protected readonly profileForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    const user = this.auth.user();
    if (user) {
      this.profileForm.patchValue({ username: user.username, email: user.email });
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    const value = this.profileForm.getRawValue();
    this.account.updateProfile(value).subscribe({
      next: (me) => {
        this.savingProfile.set(false);
        this.auth.patchUser({ username: me.username, email: me.email });
        this.notifier.success('Perfil actualizado.');
      },
      error: (p: ProblemDetail) => {
        this.savingProfile.set(false);
        this.notifier.error(p.detail ?? 'No se pudo actualizar el perfil.');
      },
    });
  }

  savePassword(): void {
    if (this.passwordForm.invalid || this.savingPassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.account.changePassword(this.passwordForm.getRawValue()).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordForm.reset();
        this.notifier.success('Contraseña actualizada.');
      },
      error: (p: ProblemDetail) => {
        this.savingPassword.set(false);
        this.notifier.error(p.detail ?? 'No se pudo cambiar la contraseña.');
      },
    });
  }

  logout(): void {
    this.auth.logout();
  }
}

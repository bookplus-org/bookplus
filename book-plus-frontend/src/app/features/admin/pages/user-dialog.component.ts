import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { AdminUser } from '../data/admin-users.service';

export type UserDialogMode = 'create' | 'edit' | 'reset';

export interface UserDialogData {
  mode: UserDialogMode;
  user?: AdminUser;
}

@Component({
  selector: 'bp-user-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="flex flex-col gap-1 pt-2" style="min-width: 360px">
        @if (data.mode !== 'reset') {
          <mat-form-field appearance="outline">
            <mat-label>Nombre de usuario</mat-label>
            <input matInput formControlName="username" autocomplete="off" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Correo</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="off" />
          </mat-form-field>
        }
        @if (data.mode === 'create' || data.mode === 'reset') {
          <mat-form-field appearance="outline">
            <mat-label>{{ data.mode === 'reset' ? 'Nueva contraseña' : 'Contraseña' }}</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="new-password" />
            <mat-hint>Mínimo 8 caracteres</mat-hint>
          </mat-form-field>
        }
        @if (data.mode === 'create') {
          <mat-form-field appearance="outline">
            <mat-label>Rol</mat-label>
            <mat-select formControlName="role">
              <mat-option value="ROLE_USER">Usuario</mat-option>
              <mat-option value="ROLE_REPARTIDOR">Repartidor</mat-option>
              <mat-option value="ROLE_EDITOR">Editor</mat-option>
              <mat-option value="ROLE_ADMIN">Admin</mat-option>
              <mat-option value="ROLE_SUPERADMIN">Superadmin</mat-option>
            </mat-select>
          </mat-form-field>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
          {{ confirmLabel }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<UserDialogComponent>);
  protected readonly data = inject<UserDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    username: [
      this.data.user?.username ?? '',
      this.data.mode === 'reset' ? [] : [Validators.required, Validators.minLength(3)],
    ],
    email: [
      this.data.user?.email ?? '',
      this.data.mode === 'reset' ? [] : [Validators.required, Validators.email],
    ],
    password: [
      '',
      this.data.mode === 'edit' ? [] : [Validators.required, Validators.minLength(8)],
    ],
    role: ['ROLE_USER', this.data.mode === 'create' ? [Validators.required] : []],
  });

  protected get title(): string {
    return this.data.mode === 'create'
      ? 'Nuevo usuario'
      : this.data.mode === 'edit'
        ? 'Editar usuario'
        : 'Restablecer contraseña';
  }

  protected get confirmLabel(): string {
    return this.data.mode === 'reset' ? 'Restablecer' : 'Guardar';
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    if (this.data.mode === 'create') {
      this.ref.close({ username: v.username, email: v.email, password: v.password, role: v.role });
    } else if (this.data.mode === 'edit') {
      this.ref.close({ username: v.username, email: v.email });
    } else {
      this.ref.close({ password: v.password });
    }
  }
}

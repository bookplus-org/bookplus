import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { AdminUser, AdminUsersService } from '../data/admin-users.service';
import { UserDialogComponent, UserDialogData } from './user-dialog.component';
import { NotificationService } from '@core/notifications/notification.service';
import { AuthStore } from '@core/auth/auth.store';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-admin-users-page',
  standalone: true,
  imports: [
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users-page.component.html',
})
export class AdminUsersPageComponent implements OnInit {
  private readonly service = inject(AdminUsersService);
  private readonly notifier = inject(NotificationService);
  private readonly auth = inject(AuthStore);
  private readonly dialog = inject(MatDialog);

  protected readonly users = signal<AdminUser[]>([]);
  protected readonly loading = signal(true);
  protected readonly busyId = signal<string | null>(null);

  protected readonly currentUserId = computed(() => this.auth.user()?.id ?? '');

  /** Manageable roles beyond the base ROLE_USER. */
  protected readonly manageableRoles = ['ROLE_REPARTIDOR', 'ROLE_EDITOR', 'ROLE_ADMIN', 'ROLE_SUPERADMIN'];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  roleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }

  openCreate(): void {
    const data: UserDialogData = { mode: 'create' };
    this.dialog.open(UserDialogComponent, { data }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.service.create(res).subscribe({
        next: () => {
          this.notifier.success('Usuario creado.');
          this.reload();
        },
        error: (p: ProblemDetail) => this.notifier.error(p.detail ?? 'No se pudo crear el usuario.'),
      });
    });
  }

  openEdit(user: AdminUser): void {
    const data: UserDialogData = { mode: 'edit', user };
    this.dialog.open(UserDialogComponent, { data }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.busyId.set(user.id);
      this.service.updateProfile(user.id, res).subscribe({
        next: (updated) => {
          this.patch(updated);
          this.notifier.success('Usuario actualizado.');
        },
        error: (p: ProblemDetail) => {
          this.busyId.set(null);
          this.notifier.error(p.detail ?? 'No se pudo actualizar.');
        },
      });
    });
  }

  openReset(user: AdminUser): void {
    const data: UserDialogData = { mode: 'reset', user };
    this.dialog.open(UserDialogComponent, { data }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.service.resetPassword(user.id, res.password).subscribe({
        next: () => this.notifier.success('Contraseña restablecida.'),
        error: (p: ProblemDetail) => this.notifier.error(p.detail ?? 'No se pudo restablecer.'),
      });
    });
  }

  hasRole(user: AdminUser, role: string): boolean {
    return user.roles.includes(role);
  }

  toggleStatus(user: AdminUser): void {
    this.busyId.set(user.id);
    this.service.setStatus(user.id, !user.enabled).subscribe({
      next: (updated) => {
        this.patch(updated);
        this.notifier.success(updated.enabled ? 'Cuenta activada.' : 'Cuenta desactivada.');
      },
      error: (p: ProblemDetail) => {
        this.busyId.set(null);
        this.notifier.error(p.detail ?? 'No se pudo actualizar la cuenta.');
      },
    });
  }

  toggleRole(user: AdminUser, role: string): void {
    const grant = !this.hasRole(user, role);
    this.busyId.set(user.id);
    this.service.changeRole(user.id, role, grant).subscribe({
      next: (updated) => {
        this.patch(updated);
        this.notifier.success(grant ? `Rol ${this.roleLabel(role)} otorgado.` : `Rol ${this.roleLabel(role)} revocado.`);
      },
      error: (p: ProblemDetail) => {
        this.busyId.set(null);
        this.notifier.error(p.detail ?? 'No se pudo cambiar el rol.');
      },
    });
  }

  private patch(updated: AdminUser): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
    this.busyId.set(null);
  }
}

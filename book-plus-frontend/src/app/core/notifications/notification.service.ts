import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Thin wrapper over MatSnackBar for consistent toast styling app-wide. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { panelClass: 'bp-snack-success' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Cerrar', { panelClass: 'bp-snack-error', duration: 6000 });
  }

  info(message: string): void {
    this.snackBar.open(message, 'OK');
  }
}

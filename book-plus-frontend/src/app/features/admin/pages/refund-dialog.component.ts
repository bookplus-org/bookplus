import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

export interface RefundDialogData {
  orderId: string;
  /** true si el pedido es físico, para mostrar/ofrecer reposición de stock. */
  physical: boolean;
  /** Hechos de consumo del libro digital (entrada de la política de reembolsos). */
  downloaded?: boolean;
  readProgress?: number;
}

export interface RefundDialogResult {
  reason: string;
  restock: boolean;
  downloaded: boolean;
  readProgress: number;
  adminOverride: boolean;
}

@Component({
  selector: 'bp-refund-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Emitir reembolso</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="flex flex-col gap-1 pt-2" style="min-width: 380px">
        <p class="mb-2 text-sm text-ink-500">
          Pedido #{{ data.orderId.slice(0, 8).toUpperCase() }} — el dinero se devuelve de forma simulada.
        </p>
        <mat-form-field appearance="outline">
          <mat-label>Motivo del reembolso</mat-label>
          <textarea matInput rows="3" formControlName="reason"
                    placeholder="Ej. Producto defectuoso / devolución acordada…"></textarea>
        </mat-form-field>

        @if (data.physical) {
          <mat-checkbox formControlName="restock" color="primary">
            Devolver los artículos al stock
          </mat-checkbox>
          <p class="ml-9 -mt-1 text-xs text-ink-400">
            Actívalo solo si el producto vuelve en buen estado y es revendible.
          </p>
        } @else {
          <div class="rounded-md bg-ink-50 p-3 text-xs text-ink-500">
            <p class="font-medium text-ink-600">Libro digital — política de reembolso</p>
            <p class="mt-1">
              Descargado: <b>{{ data.downloaded ? 'sí' : 'no' }}</b> ·
              Leído: <b>{{ data.readProgress ?? 0 }}%</b>
            </p>
            <p class="mt-1">
              Si no se ha consumido y está dentro de la ventana, se reembolsa en efectivo y se
              revoca el acceso. Si ya se consumió, se ofrece crédito en tienda. Fuera de la
              ventana se deniega (salvo override).
            </p>
          </div>
          <mat-checkbox formControlName="adminOverride" color="warn">
            Forzar reembolso en efectivo (override de administración)
          </mat-checkbox>
          <p class="ml-9 -mt-1 text-xs text-ink-400">
            Para excepciones: cobro doble, compra duplicada o archivo defectuoso.
          </p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
          <mat-icon fontSet="material-symbols-outlined">currency_exchange</mat-icon>
          Reembolsar
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class RefundDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<RefundDialogComponent>);
  protected readonly data = inject<RefundDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
    restock: [false],
    adminOverride: [false],
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const result: RefundDialogResult = {
      reason: v.reason.trim(),
      restock: this.data.physical && v.restock,
      downloaded: this.data.downloaded ?? false,
      readProgress: this.data.readProgress ?? 0,
      adminOverride: !this.data.physical && v.adminOverride,
    };
    this.ref.close(result);
  }
}

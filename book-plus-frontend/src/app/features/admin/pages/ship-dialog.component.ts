import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

const OWN_DELIVERY = 'Reparto propio (personal)';

@Component({
  selector: 'bp-ship-dialog',
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
    <h2 mat-dialog-title>Marcar como enviado</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="flex flex-col gap-1 pt-2" style="min-width: 360px">
        <mat-form-field appearance="outline">
          <mat-label>¿Quién lo entrega?</mat-label>
          <mat-select formControlName="carrier">
            @for (c of carriers; track c) {
              <mat-option [value]="c">{{ c }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (!isOwnDelivery()) {
          <mat-form-field appearance="outline">
            <mat-label>Número de seguimiento</mat-label>
            <input matInput formControlName="trackingNumber" placeholder="Ej. 1Z999AA10123456784" />
            <mat-hint>Número de guía que te entregó la agencia al despachar el paquete.</mat-hint>
          </mat-form-field>
        } @else {
          <p class="mb-2 text-sm text-ink-500">
            Reparto propio: no necesitas número de seguimiento.
          </p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Confirmar envío</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ShipDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<ShipDialogComponent>);

  protected readonly carriers = [OWN_DELIVERY, 'Olva Courier', 'Shalom', 'Serpost', 'DHL', 'FedEx', 'UPS'];

  protected readonly form = this.fb.nonNullable.group({
    carrier: [OWN_DELIVERY, [Validators.required]],
    trackingNumber: [''],
  });

  constructor() {
    this.form.controls.carrier.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((carrier) => {
        const tracking = this.form.controls.trackingNumber;
        if (carrier === OWN_DELIVERY) {
          tracking.clearValidators();
        } else {
          tracking.setValidators([Validators.required]);
        }
        tracking.updateValueAndValidity();
      });
  }

  protected isOwnDelivery(): boolean {
    return this.form.controls.carrier.value === OWN_DELIVERY;
  }

  submit(): void {
    if (this.form.valid) {
      this.ref.close(this.form.getRawValue());
    }
  }
}

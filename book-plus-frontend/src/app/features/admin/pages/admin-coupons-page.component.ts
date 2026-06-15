import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminCouponsService, Coupon } from '../data/admin-coupons.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-admin-coupons-page',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressBarModule, MatFormFieldModule, MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold">Cupones</h1>
        <p class="text-sm text-gray-500">Crea, activa o desactiva códigos de descuento.</p>
      </div>
      <button mat-stroked-button (click)="reload()" [disabled]="loading()">
        <mat-icon fontSet="material-symbols-outlined">refresh</mat-icon>
        Actualizar
      </button>
    </div>

    <div class="grid gap-6 lg:grid-cols-3">
      <!-- Form -->
      <form [formGroup]="form" (ngSubmit)="create()" class="bp-card p-5 lg:col-span-1 h-fit">
        <h2 class="mb-4 font-semibold text-ink-900">Nuevo cupón</h2>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Código</mat-label>
          <input matInput formControlName="code" placeholder="BIENVENIDO10" (input)="upper()" maxlength="40" />
          <mat-hint>Se guardará en mayúsculas.</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Tipo de descuento</mat-label>
          <mat-select formControlName="discountType">
            <mat-option value="PERCENT">Porcentaje (%)</mat-option>
            <mat-option value="FIXED">Monto fijo (S/)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>{{ form.value.discountType === 'PERCENT' ? 'Porcentaje' : 'Monto' }}</mat-label>
          <input matInput type="number" formControlName="discountValue" min="0" step="0.01" />
          <span matTextSuffix>{{ form.value.discountType === 'PERCENT' ? '%' : 'S/' }}</span>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Compra mínima (opcional)</mat-label>
          <input matInput type="number" formControlName="minAmount" min="0" step="0.01" />
          <span matTextSuffix>S/</span>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Expira (opcional)</mat-label>
          <input matInput type="date" formControlName="expiresAt" />
        </mat-form-field>

        <button mat-flat-button color="primary" class="!rounded-full w-full" [disabled]="form.invalid || saving()">
          <mat-icon fontSet="material-symbols-outlined">add</mat-icon>
          Crear cupón
        </button>
      </form>

      <!-- List -->
      <div class="lg:col-span-2">
        @if (loading()) {
          <mat-progress-bar mode="indeterminate" />
        }
        @if (!loading() && coupons().length === 0) {
          <div class="flex flex-col items-center gap-3 rounded-xl2 border border-slate-200 bg-white py-20 text-center shadow-card">
            <mat-icon fontSet="material-symbols-outlined" class="!text-5xl text-ink-200">sell</mat-icon>
            <p class="text-ink-500">Aún no hay cupones.</p>
          </div>
        } @else {
          <div class="space-y-3">
            @for (c of coupons(); track c.code) {
              <div class="bp-card flex items-center justify-between gap-4 p-4"
                   [class.opacity-60]="!c.active">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-mono font-semibold text-ink-900">{{ c.code }}</span>
                    <span class="rounded-full px-2 py-0.5 text-xs font-medium"
                          [class]="c.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                      {{ c.active ? 'Activo' : 'Inactivo' }}
                    </span>
                  </div>
                  <div class="mt-1 text-sm text-ink-600">
                    {{ c.discountType === 'PERCENT'
                        ? (c.discountValue | number: '1.0-2') + '% de descuento'
                        : 'S/ ' + (c.discountValue | number: '1.2-2') + ' de descuento' }}
                    @if (c.minAmount) { · mín. S/ {{ c.minAmount | number: '1.2-2' }} }
                    @if (c.expiresAt) { · expira {{ c.expiresAt | date: 'mediumDate' }} }
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <mat-slide-toggle [checked]="c.active" (change)="toggle(c)" />
                  <button mat-icon-button color="warn" (click)="remove(c)" matTooltip="Eliminar">
                    <mat-icon fontSet="material-symbols-outlined">delete</mat-icon>
                  </button>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class AdminCouponsPageComponent implements OnInit {
  private readonly api = inject(AdminCouponsService);
  private readonly fb = inject(FormBuilder);
  private readonly notify = inject(NotificationService);

  protected readonly coupons = signal<Coupon[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.maxLength(40)]],
    discountType: ['PERCENT' as 'PERCENT' | 'FIXED', Validators.required],
    discountValue: [10, [Validators.required, Validators.min(0.01)]],
    minAmount: [null as number | null],
    expiresAt: [null as string | null],
  });

  ngOnInit(): void {
    this.reload();
  }

  upper(): void {
    const v = this.form.controls.code.value.toUpperCase();
    this.form.controls.code.setValue(v, { emitEvent: false });
  }

  reload(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (c) => { this.coupons.set(c); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const raw = this.form.getRawValue();
    this.api.create({
      code: raw.code.trim().toUpperCase(),
      discountType: raw.discountType,
      discountValue: Number(raw.discountValue),
      minAmount: raw.minAmount ? Number(raw.minAmount) : null,
      expiresAt: raw.expiresAt ? new Date(raw.expiresAt).toISOString() : null,
    }).subscribe({
      next: () => {
        this.notify.success('Cupón creado');
        this.form.reset({ code: '', discountType: 'PERCENT', discountValue: 10, minAmount: null, expiresAt: null });
        this.saving.set(false);
        this.reload();
      },
      error: (e: { error?: ProblemDetail }) => {
        this.notify.error(e.error?.detail ?? 'No se pudo crear el cupón');
        this.saving.set(false);
      },
    });
  }

  toggle(c: Coupon): void {
    this.api.setActive(c.code, !c.active).subscribe({
      next: (updated) => {
        this.coupons.update((list) => list.map((x) => (x.code === c.code ? updated : x)));
        this.notify.success(updated.active ? 'Cupón activado' : 'Cupón desactivado');
      },
      error: () => this.notify.error('No se pudo actualizar el cupón'),
    });
  }

  remove(c: Coupon): void {
    if (!confirm(`¿Eliminar el cupón ${c.code}?`)) return;
    this.api.remove(c.code).subscribe({
      next: () => {
        this.coupons.update((list) => list.filter((x) => x.code !== c.code));
        this.notify.success('Cupón eliminado');
      },
      error: () => this.notify.error('No se pudo eliminar el cupón'),
    });
  }
}

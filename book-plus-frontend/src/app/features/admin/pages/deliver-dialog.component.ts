import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef,
  inject, signal, viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface DeliverProofResult {
  deliveryCode: string;
  receivedBy: string;
  photo: File;
  signature: Blob | null;
}

@Component({
  selector: 'bp-deliver-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Confirmar entrega</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="flex flex-col gap-1 pt-2" style="min-width: 380px; max-width: 440px">
        <p class="mb-2 flex items-start gap-2 text-sm text-ink-500">
          <mat-icon fontSet="material-symbols-outlined" class="!text-base text-brand-600">lock</mat-icon>
          Pide al cliente el código de entrega que ve en su pedido y escríbelo aquí.
        </p>
        <mat-form-field appearance="outline">
          <mat-label>Código de entrega</mat-label>
          <input matInput formControlName="deliveryCode" maxlength="6" placeholder="6 dígitos" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Recibido por (nombre)</mat-label>
          <input matInput formControlName="receivedBy" placeholder="Nombre de quien recibe" />
        </mat-form-field>

        <!-- Foto de la entrega -->
        <label class="mt-1 text-sm font-medium text-ink-700">Foto de la entrega *</label>
        <div class="mt-1 flex items-center gap-3">
          <button mat-stroked-button type="button" class="!rounded-full" (click)="fileInput.click()">
            <mat-icon fontSet="material-symbols-outlined">photo_camera</mat-icon>
            {{ photoUrl() ? 'Cambiar foto' : 'Tomar / subir foto' }}
          </button>
          @if (photoUrl()) {
            <img [src]="photoUrl()" alt="Foto" class="h-16 w-16 rounded-lg object-cover ring-1 ring-slate-200" />
          }
        </div>
        <input #fileInput type="file" accept="image/*" capture="environment" class="hidden"
               (change)="onPhoto($event)" />

        <!-- Firma -->
        <label class="mt-3 text-sm font-medium text-ink-700">Firma del receptor</label>
        <div class="mt-1 rounded-lg border border-dashed border-slate-300 bg-slate-50">
          <canvas #pad width="400" height="140" class="w-full touch-none rounded-lg bg-white"></canvas>
        </div>
        <button mat-button type="button" class="self-end !text-xs" (click)="clearSignature()">
          <mat-icon fontSet="material-symbols-outlined" class="!text-sm">ink_eraser</mat-icon>
          Limpiar firma
        </button>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || !photoFile()">
          Confirmar entrega
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class DeliverDialogComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<DeliverDialogComponent>);

  private readonly pad = viewChild.required<ElementRef<HTMLCanvasElement>>('pad');

  protected readonly photoUrl = signal<string | null>(null);
  protected readonly photoFile = signal<File | null>(null);

  private drawing = false;
  private hasSignature = false;

  protected readonly form = this.fb.nonNullable.group({
    deliveryCode: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
    receivedBy: ['', [Validators.required]],
  });

  ngAfterViewInit(): void {
    const canvas = this.pad().nativeElement;
    const ctx = canvas.getContext('2d')!;
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1e293b';

    const pos = (e: PointerEvent) => {
      const r = canvas.getBoundingClientRect();
      return { x: (e.clientX - r.left) * (canvas.width / r.width),
               y: (e.clientY - r.top) * (canvas.height / r.height) };
    };
    canvas.addEventListener('pointerdown', (e) => {
      this.drawing = true; this.hasSignature = true;
      const p = pos(e); ctx.beginPath(); ctx.moveTo(p.x, p.y);
      canvas.setPointerCapture(e.pointerId);
    });
    canvas.addEventListener('pointermove', (e) => {
      if (!this.drawing) return;
      const p = pos(e); ctx.lineTo(p.x, p.y); ctx.stroke();
    });
    const stop = () => { this.drawing = false; };
    canvas.addEventListener('pointerup', stop);
    canvas.addEventListener('pointerleave', stop);
  }

  onPhoto(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.photoFile.set(file);
    this.photoUrl.set(URL.createObjectURL(file));
  }

  clearSignature(): void {
    const canvas = this.pad().nativeElement;
    canvas.getContext('2d')!.clearRect(0, 0, canvas.width, canvas.height);
    this.hasSignature = false;
  }

  submit(): void {
    const file = this.photoFile();
    if (this.form.invalid || !file) return;
    const v = this.form.getRawValue();

    const finish = (signature: Blob | null) => {
      this.ref.close({ deliveryCode: v.deliveryCode, receivedBy: v.receivedBy, photo: file, signature });
    };

    if (this.hasSignature) {
      this.pad().nativeElement.toBlob((blob) => finish(blob), 'image/png');
    } else {
      finish(null);
    }
  }
}

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface PromptDialogData {
  title: string;
  label: string;
  placeholder?: string;
  confirmLabel?: string;
  initialValue?: string;
}

@Component({
  selector: 'bp-prompt-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content style="min-width: 360px">
      <mat-form-field appearance="outline" class="w-full pt-2">
        <mat-label>{{ data.label }}</mat-label>
        <textarea matInput rows="3" [formControl]="text" [placeholder]="data.placeholder ?? ''"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" [disabled]="text.invalid" (click)="confirm()">
        {{ data.confirmLabel ?? 'Enviar' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class PromptDialogComponent {
  private readonly ref = inject(MatDialogRef<PromptDialogComponent>);
  protected readonly data = inject<PromptDialogData>(MAT_DIALOG_DATA);
  protected readonly text = new FormControl(this.data.initialValue ?? '', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });

  confirm(): void {
    if (this.text.valid) {
      this.ref.close(this.text.value.trim());
    }
  }
}

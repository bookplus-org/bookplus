import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import { ReportService, SalesMetric, SalesSummary, TopBook } from '../data/report.service';
import { NotificationService } from '@core/notifications/notification.service';

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

@Component({
  selector: 'bp-admin-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent {
  private readonly reports = inject(ReportService);
  private readonly notifier = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly summary = signal<SalesSummary | null>(null);
  protected readonly daily = signal<SalesMetric[]>([]);
  protected readonly topBooks = signal<TopBook[]>([]);

  protected readonly range = this.fb.nonNullable.group({
    from: [isoDaysAgo(30)],
    to: [isoDaysAgo(0)],
  });

  /** Pre-computed bars for the inline SVG revenue chart. */
  protected readonly bars = computed(() => {
    const data = this.daily();
    const max = Math.max(1, ...data.map((d) => d.revenue));
    const width = 100 / Math.max(1, data.length);
    return data.map((d, i) => ({
      x: i * width,
      width: width * 0.8,
      height: (d.revenue / max) * 100,
      y: 100 - (d.revenue / max) * 100,
      date: d.date,
      revenue: d.revenue,
    }));
  });

  constructor() {
    this.load();
  }

  load(): void {
    const { from, to } = this.range.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      summary: this.reports.summary(from, to),
      daily: this.reports.daily(from, to),
      topBooks: this.reports.topBooks(from, to, 10),
    }).subscribe({
      next: ({ summary, daily, topBooks }) => {
        this.summary.set(summary);
        this.daily.set(daily);
        this.topBooks.set(topBooks);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los reportes.');
        this.loading.set(false);
      },
    });
  }

  exportCsv(): void {
    const { from, to } = this.range.getRawValue();
    this.reports.exportCsv(from, to).subscribe({
      next: (blob) => this.download(blob, `ventas_${from}_${to}.csv`),
      error: () => this.notifier.error('No se pudo exportar el CSV.'),
    });
  }

  exportPdf(): void {
    const { from, to } = this.range.getRawValue();
    this.reports.exportPdf(from, to).subscribe({
      next: (blob) => this.download(blob, `ventas_${from}_${to}.pdf`),
      error: () => this.notifier.error('No se pudo exportar el PDF.'),
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}

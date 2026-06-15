import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { startAutoRefresh } from '@core/util/auto-refresh';
import { AdminNotificationsService, AdminNotification } from '../data/admin-notifications.service';

const TYPE_LABEL: Record<string, string> = {
  ORDER_CREATED: 'Pedido creado',
  ORDER_CONFIRMED: 'Pedido confirmado',
  ORDER_SHIPPED: 'Pedido enviado',
  ORDER_DELIVERED: 'Pedido entregado',
  ORDER_CANCELLED: 'Pedido cancelado',
  ORDER_REFUNDED: 'Pedido reembolsado',
  PAYMENT_COMPLETED: 'Pago completado',
  PAYMENT_FAILED: 'Pago fallido',
  PAYMENT_REFUNDED: 'Pago reembolsado',
  LOW_STOCK_ALERT: 'Stock bajo',
  REVIEW_ADDED: 'Reseña añadida',
};

@Component({
  selector: 'bp-admin-notifications-page',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule, MatPaginatorModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold">Notificaciones enviadas</h1>
        <p class="text-sm text-gray-500">Historial de correos y avisos emitidos por el sistema.</p>
      </div>
      <button mat-stroked-button (click)="reload()" [disabled]="loading()">
        <mat-icon fontSet="material-symbols-outlined">refresh</mat-icon>
        Actualizar
      </button>
    </div>

    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }

    @if (!loading() && items().length === 0) {
      <div class="flex flex-col items-center gap-3 rounded-xl2 border border-slate-200 bg-white py-20 text-center shadow-card">
        <mat-icon fontSet="material-symbols-outlined" class="!text-5xl text-ink-200">mark_email_read</mat-icon>
        <p class="text-ink-500">Aún no se ha enviado ninguna notificación.</p>
      </div>
    } @else {
      <div class="overflow-x-auto rounded-xl2 border border-slate-200 bg-white shadow-card">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-200 text-left text-ink-500">
            <tr>
              <th class="px-4 py-3 font-medium">Tipo</th>
              <th class="px-4 py-3 font-medium">Asunto</th>
              <th class="px-4 py-3 font-medium">Destinatario</th>
              <th class="px-4 py-3 font-medium">Canal</th>
              <th class="px-4 py-3 font-medium">Estado</th>
              <th class="px-4 py-3 font-medium">Fecha</th>
            </tr>
          </thead>
          <tbody>
            @for (n of items(); track n.id) {
              <tr class="border-b border-slate-100 last:border-0">
                <td class="px-4 py-3">{{ typeLabel(n.type) }}</td>
                <td class="px-4 py-3 text-ink-700">{{ n.subject }}</td>
                <td class="px-4 py-3 font-mono text-xs text-ink-500">{{ n.recipientEmail || '—' }}</td>
                <td class="px-4 py-3 text-ink-500">{{ n.channel }}</td>
                <td class="px-4 py-3">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" [class]="statusClass(n.status)">
                    {{ n.status }}
                  </span>
                </td>
                <td class="px-4 py-3 text-ink-500">{{ (n.sentAt || n.createdAt) | date: 'short' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <mat-paginator
        [length]="total()"
        [pageSize]="pageSize"
        [pageIndex]="pageIndex()"
        [pageSizeOptions]="[20]"
        (page)="onPage($event)"
      />
    }
  `,
})
export class AdminNotificationsPageComponent implements OnInit {
  private readonly api = inject(AdminNotificationsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly items = signal<AdminNotification[]>([]);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(false);
  protected readonly pageSize = 20;

  ngOnInit(): void {
    this.fetch();
    startAutoRefresh(15000, this.destroyRef, () => this.fetch(false));
  }

  reload(): void {
    this.fetch();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.fetch();
  }

  typeLabel(type: string): string {
    return TYPE_LABEL[type] ?? type;
  }

  statusClass(status: string): string {
    switch (status) {
      case 'SENT': return 'bg-emerald-50 text-emerald-700';
      case 'FAILED': return 'bg-rose-50 text-rose-600';
      case 'PENDING': return 'bg-amber-50 text-amber-700';
      default: return 'bg-slate-100 text-ink-500';
    }
  }

  private fetch(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.api.list(this.pageIndex(), this.pageSize).subscribe({
      next: (page) => {
        this.items.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

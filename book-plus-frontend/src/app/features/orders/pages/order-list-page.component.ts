import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { startAutoRefresh } from '@core/util/auto-refresh';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrdersService } from '../data/orders.service';
import { ReceiptService } from '../data/receipt.service';
import { OrderEventsService } from '../data/order-events.service';
import { ORDER_STATUS_LABEL, Order } from '../models/order.model';
import { AsyncState, failure, loading, success } from '@core/models/async-state.model';
import { Page } from '@core/models/page.model';
import { ProblemDetail } from '@core/models/problem-detail.model';

const STATUS_CLASS: Record<string, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-700',
  PAYMENT_PROCESSING: 'bg-blue-100 text-blue-700',
  CONFIRMED: 'bg-indigo-100 text-indigo-700',
  SHIPPED: 'bg-violet-100 text-violet-700',
  DELIVERED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-rose-100 text-rose-700',
  REFUNDED: 'bg-orange-100 text-orange-700',
};

@Component({
  selector: 'bp-order-list-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './order-list-page.component.html',
})
export class OrderListPageComponent {
  private readonly orders = inject(OrdersService);
  private readonly receipts = inject(ReceiptService);
  private readonly events = inject(OrderEventsService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly state = signal<AsyncState<Page<Order>>>(loading());
  protected readonly label = ORDER_STATUS_LABEL;

  protected statusClass(status: string): string {
    return STATUS_CLASS[status] ?? 'bg-slate-100 text-ink-500';
  }

  protected downloadReceipt(event: Event, order: Order): void {
    event.preventDefault();
    event.stopPropagation();
    this.receipts.download(order);
  }

  constructor() {
    this.load(true);
    startAutoRefresh(12000, this.destroyRef, () => this.load(false));
    this.events.stream()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(false));
  }

  private load(showSpinner: boolean): void {
    if (showSpinner) this.state.set(loading());
    this.orders.list({ page: 0, size: 20, sort: 'createdAt,desc' }).subscribe({
      next: (page) => this.state.set(success(page)),
      error: (problem: ProblemDetail) => {
        if (showSpinner) {
          this.state.set(failure(problem.detail ?? 'No se pudieron cargar tus pedidos.'));
        }
      },
    });
  }
}

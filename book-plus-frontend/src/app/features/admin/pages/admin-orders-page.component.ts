import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { startAutoRefresh } from '@core/util/auto-refresh';
import { OrderEventsService } from '@features/orders/data/order-events.service';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog } from '@angular/material/dialog';
import { AdminOrdersService, PurchaseConsumption } from '../data/admin-orders.service';
import {
  CANCELLABLE_STATUSES,
  REFUNDABLE_STATUSES,
  ORDER_STATUS_LABEL,
  Order,
  OrderStatus,
} from '@features/orders/models/order.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { ShipDialogComponent } from './ship-dialog.component';
import { DeliverDialogComponent } from './deliver-dialog.component';
import {
  RefundDialogComponent,
  RefundDialogData,
  RefundDialogResult,
} from './refund-dialog.component';
import {
  PromptDialogComponent,
  PromptDialogData,
} from '@shared/ui/prompt-dialog/prompt-dialog.component';

const STATUS_CLASS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'bg-amber-50 text-amber-700',
  PAYMENT_PROCESSING: 'bg-blue-50 text-blue-700',
  CONFIRMED: 'bg-indigo-50 text-indigo-700',
  SHIPPED: 'bg-violet-50 text-violet-700',
  DELIVERED: 'bg-green-50 text-green-700',
  CANCELLED: 'bg-red-50 text-red-600',
  REFUNDED: 'bg-orange-50 text-orange-700',
};

const PAGE_SIZE = 20;

@Component({
  selector: 'bp-admin-orders-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-orders-page.component.html',
})
export class AdminOrdersPageComponent implements OnInit {
  private readonly service = inject(AdminOrdersService);
  private readonly notifier = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly events = inject(OrderEventsService);

  protected readonly label = ORDER_STATUS_LABEL;
  protected readonly statuses: OrderStatus[] = [
    'PENDING_PAYMENT',
    'PAYMENT_PROCESSING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED',
  ];

  protected readonly statusFilter = new FormControl<OrderStatus | ''>('', { nonNullable: true });
  protected readonly orders = signal<Order[]>([]);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(false);
  protected readonly busyId = signal<string | null>(null);
  protected readonly pageSize = PAGE_SIZE;

  ngOnInit(): void {
    this.fetch();
    this.statusFilter.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.fetch();
    });
    startAutoRefresh(12000, this.destroyRef, () => this.fetch(false));
    this.events.stream()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.fetch(false));
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.fetch();
  }

  statusClass(status: OrderStatus): string {
    return STATUS_CLASS[status];
  }

  canShip(o: Order): boolean {
    return o.status === 'CONFIRMED' && o.deliveryType !== 'DIGITAL';
  }
  canDeliver(o: Order): boolean {
    return o.status === 'SHIPPED' && o.deliveryType !== 'DIGITAL';
  }
  canCancel(o: Order): boolean {
    return CANCELLABLE_STATUSES.has(o.status);
  }
  canRefund(o: Order): boolean {
    return REFUNDABLE_STATUSES.has(o.status);
  }

  ship(o: Order): void {
    this.dialog.open(ShipDialogComponent, { width: '420px' }).afterClosed().subscribe((res) => {
      if (res) {
        this.run(this.service.ship(o.orderId, res), o.orderId, 'Pedido marcado como enviado.');
      }
    });
  }
  deliver(o: Order): void {
    this.dialog.open(DeliverDialogComponent, { width: '420px' }).afterClosed().subscribe((res) => {
      if (res) {
        this.run(this.service.deliverWithProof(o.orderId, res), o.orderId, 'Pedido marcado como entregado.');
      }
    });
  }

  cancel(o: Order): void {
    const data: ConfirmDialogData = {
      title: 'Cancelar pedido',
      message: `¿Cancelar el pedido #${o.orderId.slice(0, 8)}?`,
      confirmLabel: 'Cancelar pedido',
      danger: true,
    };
    this.dialog.open(ConfirmDialogComponent, { data, width: '420px' }).afterClosed().subscribe((ok) => {
      if (ok) {
        this.run(this.service.cancel(o.orderId, 'Cancelado por un administrador'), o.orderId, 'Pedido cancelado.');
      }
    });
  }

  refund(o: Order): void {
    const physical = o.deliveryType !== 'DIGITAL';
    if (physical) {
      this.openRefundDialog(o, { orderId: o.orderId, physical });
      return;
    }
    // Digital: traer los hechos de consumo de catalog y agregarlos (descargado = alguno,
    // progreso = máximo) para que la política decida con datos reales.
    const items = o.items ?? [];
    if (!items.length) {
      this.openRefundDialog(o, { orderId: o.orderId, physical: false, downloaded: false, readProgress: 0 });
      return;
    }
    this.busyId.set(o.orderId);
    const facts$ = items.map((it) =>
      this.service.consumption(o.userId, it.bookId).pipe(
        catchError(() => of<PurchaseConsumption>({ downloaded: false, readProgress: 0, active: true })),
      ),
    );
    forkJoin(facts$).subscribe((list) => {
      this.busyId.set(null);
      const downloaded = list.some((f) => f.downloaded);
      const readProgress = list.reduce((max, f) => Math.max(max, f.readProgress), 0);
      this.openRefundDialog(o, { orderId: o.orderId, physical: false, downloaded, readProgress });
    });
  }

  private openRefundDialog(o: Order, data: RefundDialogData): void {
    this.dialog.open(RefundDialogComponent, { data, width: '460px' }).afterClosed()
      .subscribe((res: RefundDialogResult | undefined) => {
        if (!res) return;
        this.busyId.set(o.orderId);
        this.service.refund(o.orderId, res).subscribe({
          next: (r) => {
            this.orders.update((arr) => arr.map((x) => (x.orderId === o.orderId ? r.order : x)));
            this.busyId.set(null);
            if (r.outcome === 'STORE_CREDIT') {
              this.notifier.success(`Crédito en tienda emitido: ${r.storeCreditCode}`);
            } else {
              this.notifier.success('Reembolso en efectivo emitido.');
            }
          },
          error: (p: ProblemDetail) => {
            this.busyId.set(null);
            this.notifier.error(p.detail ?? 'No se pudo emitir el reembolso.');
          },
        });
      });
  }

  resolveClaim(o: Order): void {
    const data: PromptDialogData = {
      title: 'Resolver reclamo',
      label: `Reclamo: "${o.claimReason ?? ''}" — escribe la resolución`,
      placeholder: 'Ej. Reenvío programado / Reembolso emitido…',
      confirmLabel: 'Marcar resuelto',
    };
    this.dialog.open(PromptDialogComponent, { data, width: '460px' }).afterClosed().subscribe((note) => {
      if (note) {
        this.run(this.service.resolveClaim(o.orderId, note), o.orderId, 'Reclamo resuelto.');
      }
    });
  }

  private run(obs: ReturnType<AdminOrdersService['ship']>, id: string, okMsg: string): void {
    this.busyId.set(id);
    obs.subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((x) => (x.orderId === id ? updated : x)));
        this.busyId.set(null);
        this.notifier.success(okMsg);
      },
      error: (p: ProblemDetail) => {
        this.busyId.set(null);
        this.notifier.error(p.detail ?? 'No se pudo actualizar el pedido.');
      },
    });
  }

  private fetch(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.service.listAll(this.statusFilter.value, this.pageIndex(), this.pageSize).subscribe({
      next: (page) => {
        // Evita pisar una acción en curso (ship/deliver) con datos viejos del polling.
        if (this.busyId()) return;
        this.orders.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

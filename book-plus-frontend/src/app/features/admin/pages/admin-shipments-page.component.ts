import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { startAutoRefresh } from '@core/util/auto-refresh';
import { OrderEventsService } from '@features/orders/data/order-events.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog } from '@angular/material/dialog';
import { AdminOrdersService } from '../data/admin-orders.service';
import { ORDER_STATUS_LABEL, Order } from '@features/orders/models/order.model';
import { NotificationService } from '@core/notifications/notification.service';
import { AuthStore } from '@core/auth/auth.store';
import { ProblemDetail } from '@core/models/problem-detail.model';
import { ShipDialogComponent } from './ship-dialog.component';
import { DeliverDialogComponent } from './deliver-dialog.component';

@Component({
  selector: 'bp-admin-shipments-page',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-semibold">Envíos</h1>
        <p class="text-sm text-gray-500">Pedidos físicos por enviar y entregar.</p>
      </div>
      <button mat-stroked-button (click)="reload()" [disabled]="loading()">
        <mat-icon fontSet="material-symbols-outlined">refresh</mat-icon>
        Actualizar
      </button>
    </div>

    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }

    @if (!loading() && orders().length === 0) {
      <div class="flex flex-col items-center gap-3 rounded-xl2 border border-slate-200 bg-white py-20 text-center shadow-card">
        <mat-icon fontSet="material-symbols-outlined" class="!text-5xl text-ink-200">local_shipping</mat-icon>
        <p class="text-ink-500">No hay envíos pendientes.</p>
      </div>
    } @else {
      <div class="grid gap-4 lg:grid-cols-2">
        @for (o of orders(); track o.orderId) {
          <div class="bp-card p-5">
            <div class="flex items-start justify-between">
              <div>
                <div class="font-semibold text-ink-900">#{{ o.orderId.slice(0, 8) }}</div>
                <div class="text-xs text-ink-400">{{ o.createdAt | date: 'short' }}</div>
              </div>
              <span
                class="rounded-full px-2.5 py-1 text-xs font-medium"
                [class]="o.status === 'CONFIRMED' ? 'bg-indigo-50 text-indigo-700' : 'bg-violet-50 text-violet-700'"
              >{{ label[o.status] }}</span>
            </div>

            @if (o.shippingAddress; as a) {
              <p class="mt-3 text-sm text-ink-600">
                <mat-icon fontSet="material-symbols-outlined" class="!text-sm align-middle">place</mat-icon>
                {{ a.recipientName }} — {{ a.street }}, {{ a.city }}, {{ a.country }}
              </p>
            }

            @if (o.trackingNumber) {
              <p class="mt-1 text-xs text-ink-500">{{ o.carrier }} · {{ o.trackingNumber }}</p>
            }

            <div class="mt-4 flex flex-wrap items-center gap-2">
              @if (canClaim(o)) {
                <button mat-flat-button color="primary" class="!rounded-full" [disabled]="busyId() === o.orderId" (click)="claim(o)">
                  <mat-icon fontSet="material-symbols-outlined">add_task</mat-icon>
                  Tomar pedido
                </button>
              } @else if (canAct(o)) {
                @if (o.status === 'CONFIRMED') {
                  <button mat-flat-button color="primary" class="!rounded-full" [disabled]="busyId() === o.orderId" (click)="ship(o)">
                    <mat-icon fontSet="material-symbols-outlined">local_shipping</mat-icon>
                    Marcar enviado
                  </button>
                } @else if (o.status === 'SHIPPED') {
                  <button mat-stroked-button class="!rounded-full" [disabled]="busyId() === o.orderId" (click)="deliver(o)">
                    <mat-icon fontSet="material-symbols-outlined">done_all</mat-icon>
                    Marcar entregado
                  </button>
                }
              }
              @if (o.assignedCourier) {
                <span class="inline-flex items-center gap-1 text-xs text-ink-400">
                  <mat-icon fontSet="material-symbols-outlined" class="!text-sm">person</mat-icon>
                  {{ canAct(o) && !isAdmin() ? 'Asignado a ti' : ('Asignado a ' + (o.assignedCourierName || 'repartidor')) }}
                </span>
              }
            </div>
          </div>
        }
      </div>
    }
  `,
})
export class AdminShipmentsPageComponent implements OnInit {
  private readonly service = inject(AdminOrdersService);
  private readonly notifier = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly events = inject(OrderEventsService);

  protected readonly label = ORDER_STATUS_LABEL;
  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly busyId = signal<string | null>(null);

  protected readonly isAdmin = this.auth.isAdmin;
  private readonly myId = computed(() => this.auth.user()?.id ?? '');

  /** ¿Puedo enviar/entregar este pedido? (admin, o repartidor que lo tomó) */
  canAct(o: Order): boolean {
    return this.isAdmin() || o.assignedCourier === this.myId();
  }

  /** ¿Mostrar 'Tomar'? (repartidor, pedido sin asignar) */
  canClaim(o: Order): boolean {
    return !this.isAdmin() && !o.assignedCourier;
  }

  ngOnInit(): void {
    this.reload();
    startAutoRefresh(12000, this.destroyRef, () => this.reload(false));
    this.events.stream()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(false));
  }

  claim(o: Order): void {
    this.busyId.set(o.orderId);
    this.service.claimDelivery(o.orderId).subscribe({
      next: () => this.afterChange('Pedido tomado. Está en tus entregas.'),
      error: (p: ProblemDetail) => this.fail(p),
    });
  }

  reload(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.service.shipmentsQueue().subscribe({
      next: (orders) => {
        if (this.busyId()) return; // no pisar una acción en curso
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  ship(o: Order): void {
    this.dialog.open(ShipDialogComponent, { width: '420px' }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.busyId.set(o.orderId);
      this.service.ship(o.orderId, res).subscribe({
        next: () => this.afterChange('Pedido marcado como enviado.'),
        error: (p: ProblemDetail) => this.fail(p),
      });
    });
  }

  deliver(o: Order): void {
    this.dialog.open(DeliverDialogComponent, { width: '420px' }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.busyId.set(o.orderId);
      this.service.deliverWithProof(o.orderId, res).subscribe({
        next: () => this.afterChange('Pedido marcado como entregado.'),
        error: (p: ProblemDetail) => this.fail(p),
      });
    });
  }

  private afterChange(msg: string): void {
    this.busyId.set(null);
    this.notifier.success(msg);
    this.reload();
  }

  private fail(p: ProblemDetail): void {
    this.busyId.set(null);
    this.notifier.error(p.detail ?? 'No se pudo actualizar el envío.');
  }
}

import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { startAutoRefresh } from '@core/util/auto-refresh';
import { CurrencyPipe, DatePipe, UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrdersService } from '../data/orders.service';
import { ReceiptService } from '../data/receipt.service';
import { OrderEventsService } from '../data/order-events.service';
import {
  PromptDialogComponent,
  PromptDialogData,
} from '@shared/ui/prompt-dialog/prompt-dialog.component';
import {
  CANCELLABLE_STATUSES,
  ORDER_STATUS_LABEL,
  PAYMENT_METHOD_LABEL,
  Order,
  OrderStatus,
} from '../models/order.model';
import { AsyncState, failure, loading, success } from '@core/models/async-state.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-order-detail-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    UpperCasePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './order-detail-page.component.html',
})
export class OrderDetailPageComponent {
  private readonly orders = inject(OrdersService);
  private readonly receipts = inject(ReceiptService);
  private readonly events = inject(OrderEventsService);
  private readonly notifier = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();
  protected readonly state = signal<AsyncState<Order>>(loading());
  protected readonly label = ORDER_STATUS_LABEL;
  protected readonly paymentLabel = PAYMENT_METHOD_LABEL;

  protected readonly proofPhotoUrl = signal<string | null>(null);
  protected readonly proofSignatureUrl = signal<string | null>(null);

  private readonly physicalSteps: { status: OrderStatus; label: string; icon: string }[] = [
    { status: 'PENDING_PAYMENT', label: 'Pedido realizado', icon: 'receipt_long' },
    { status: 'PAYMENT_PROCESSING', label: 'Procesando pago', icon: 'credit_card' },
    { status: 'CONFIRMED', label: 'Confirmado', icon: 'check_circle' },
    { status: 'SHIPPED', label: 'Enviado', icon: 'local_shipping' },
    { status: 'DELIVERED', label: 'Entregado', icon: 'inventory_2' },
  ];

  private readonly digitalSteps: { status: OrderStatus; label: string; icon: string }[] = [
    { status: 'PENDING_PAYMENT', label: 'Pedido realizado', icon: 'receipt_long' },
    { status: 'PAYMENT_PROCESSING', label: 'Procesando pago', icon: 'credit_card' },
    { status: 'CONFIRMED', label: 'Disponible', icon: 'cloud_download' },
  ];

  steps(order: Order): { status: OrderStatus; label: string; icon: string }[] {
    return order.deliveryType === 'DIGITAL' ? this.digitalSteps : this.physicalSteps;
  }

  currentStep(order: Order): number {
    return this.steps(order).findIndex((s) => s.status === order.status);
  }

  isDigital(order: Order): boolean {
    return order.deliveryType === 'DIGITAL';
  }

  isCancelled(order: Order): boolean {
    return order.status === 'CANCELLED';
  }

  isRefunded(order: Order): boolean {
    return order.status === 'REFUNDED';
  }

  constructor() {
    effect(() => this.load(this.id()));
    startAutoRefresh(12000, this.destroyRef, () => this.refresh());
    // Refresco instantáneo cuando llega un cambio por SSE para este pedido.
    this.events.stream()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((u) => { if (!u.orderId || u.orderId === this.id()) this.refresh(); });
  }

  /** Refresco silencioso (sin spinner) usado por el polling. */
  private refresh(): void {
    const id = this.id();
    if (!id) return;
    this.orders.get(id).subscribe({
      next: (order) => {
        const s = this.state();
        const prev = s.status === 'success' ? s.data : null;
        this.state.set(success(order));
        if (!prev || prev.status !== order.status) this.loadProof(order);
      },
      error: () => {},
    });
  }

  canCancel(order: Order): boolean {
    return CANCELLABLE_STATUSES.has(order.status);
  }

  canClaim(order: Order): boolean {
    return (
      order.deliveryType === 'PHYSICAL' &&
      (order.status === 'SHIPPED' || order.status === 'DELIVERED') &&
      order.claimStatus !== 'OPEN'
    );
  }

  openClaim(order: Order): void {
    const data: PromptDialogData = {
      title: 'Reportar un problema',
      label: '¿Qué ocurrió con tu pedido?',
      placeholder: 'Ej. No he recibido el paquete / llegó dañado…',
      confirmLabel: 'Enviar reclamo',
    };
    this.dialog.open(PromptDialogComponent, { data, width: '460px' }).afterClosed().subscribe((reason) => {
      if (!reason) return;
      this.orders.openClaim(order.orderId, reason).subscribe({
        next: (updated) => {
          this.state.set(success(updated));
          this.notifier.success('Reclamo enviado. Lo revisaremos pronto.');
        },
        error: (problem: ProblemDetail) =>
          this.notifier.error(problem.detail ?? 'No se pudo enviar el reclamo.'),
      });
    });
  }

  downloadReceipt(order: Order): void {
    this.receipts.download(order);
  }

  confirmReceipt(order: Order): void {
    this.orders.confirmReceipt(order.orderId).subscribe({
      next: (updated) => {
        this.state.set(success(updated));
        this.notifier.success('¡Gracias! Confirmaste la recepción.');
      },
      error: (problem: ProblemDetail) =>
        this.notifier.error(problem.detail ?? 'No se pudo confirmar la recepción.'),
    });
  }

  cancel(order: Order): void {
    this.orders.cancel(order.orderId, 'Cancelado por el usuario').subscribe({
      next: (updated) => {
        this.state.set(success(updated));
        this.notifier.success('Pedido cancelado.');
      },
      error: (problem: ProblemDetail) =>
        this.notifier.error(problem.detail ?? 'No se pudo cancelar el pedido.'),
    });
  }

  private load(id: string): void {
    this.state.set(loading());
    this.orders.get(id).subscribe({
      next: (order) => {
        this.state.set(success(order));
        this.loadProof(order);
      },
      error: (problem: ProblemDetail) =>
        this.state.set(failure(problem.detail ?? 'No se encontró el pedido.')),
    });
  }

  private loadProof(order: Order): void {
    this.proofPhotoUrl.set(null);
    this.proofSignatureUrl.set(null);
    if (order.status !== 'DELIVERED' || order.deliveryType !== 'PHYSICAL') return;

    this.orders.proofPhoto(order.orderId).subscribe({
      next: (blob) => { if (blob.size > 0) this.proofPhotoUrl.set(URL.createObjectURL(blob)); },
      error: () => {},
    });
    this.orders.proofSignature(order.orderId).subscribe({
      next: (blob) => { if (blob.size > 0) this.proofSignatureUrl.set(URL.createObjectURL(blob)); },
      error: () => {},
    });
  }
}

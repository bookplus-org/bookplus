import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CartStore } from '@features/cart/data/cart.store';
import { CartService } from '@features/cart/data/cart.service';
import { DeliveryType, PaymentMethod } from '@features/cart/models/cart.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';
import { applyServerErrors } from '@shared/forms/apply-server-errors';

interface MethodOption {
  id: PaymentMethod;
  label: string;
  icon: string;
  desc: string;
}

@Component({
  selector: 'bp-checkout-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './checkout-page.component.html',
})
export class CheckoutPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly notifier = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly cart = inject(CartStore);
  protected readonly submitting = signal(false);
  protected readonly method = signal<PaymentMethod>('YAPE');
  protected readonly delivery = signal<DeliveryType>('DIGITAL');
  protected readonly isPhysical = computed(() => this.delivery() === 'PHYSICAL');

  setDelivery(d: DeliveryType): void {
    this.delivery.set(d);
  }

  protected readonly methods: MethodOption[] = [
    { id: 'YAPE', label: 'Yape', icon: 'qr_code_2', desc: 'Escanea el QR y paga al instante' },
    { id: 'PLIN', label: 'Plin', icon: 'qr_code', desc: 'Paga con tu app bancaria' },
    { id: 'CARD', label: 'Tarjeta', icon: 'credit_card', desc: 'Crédito o débito' },
    { id: 'PAYPAL', label: 'PayPal', icon: 'account_balance_wallet', desc: 'Pago internacional' },
    { id: 'CASH', label: 'Efectivo', icon: 'local_shipping', desc: 'Paga al recibir' },
  ];

  protected readonly isWallet = computed(() => this.method() === 'YAPE' || this.method() === 'PLIN');

  protected readonly address = this.fb.nonNullable.group({
    recipientName: ['', [Validators.required]],
    street: ['', [Validators.required]],
    city: ['', [Validators.required]],
    state: [''],
    postalCode: ['', [Validators.required]],
    country: ['Perú', [Validators.required]],
  });

  protected readonly card = this.fb.nonNullable.group({
    number: ['', [Validators.required, Validators.pattern(/^[0-9 ]{16,19}$/)]],
    holder: ['', [Validators.required]],
    expiry: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  protected readonly operation = this.fb.nonNullable.control('');
  protected readonly paypalEmail = this.fb.nonNullable.control('', [Validators.email]);
  protected readonly coupon = this.fb.nonNullable.control('');

  protected readonly discount = signal(0);
  protected readonly appliedCoupon = signal<string | null>(null);
  protected readonly couponMsg = signal<string | null>(null);
  protected readonly applyingCoupon = signal(false);
  protected readonly finalTotal = computed(() => Math.max(0, this.cart.subtotal() - this.discount()));

  applyCoupon(): void {
    const code = this.coupon.value.trim();
    if (!code) {
      return;
    }
    this.applyingCoupon.set(true);
    this.cartService.validateCoupon(code, this.cart.subtotal()).subscribe({
      next: (res) => {
        this.applyingCoupon.set(false);
        if (res.valid) {
          this.discount.set(res.discount);
          this.appliedCoupon.set(res.code);
          this.couponMsg.set(null);
        } else {
          this.discount.set(0);
          this.appliedCoupon.set(null);
          this.couponMsg.set(res.message ?? 'Cupón no válido.');
        }
      },
      error: () => {
        this.applyingCoupon.set(false);
        this.discount.set(0);
        this.appliedCoupon.set(null);
        this.couponMsg.set('No se pudo validar el cupón.');
      },
    });
  }

  removeCoupon(): void {
    this.discount.set(0);
    this.appliedCoupon.set(null);
    this.couponMsg.set(null);
    this.coupon.setValue('');
  }

  select(id: PaymentMethod): void {
    this.method.set(id);
  }

  confirm(): void {
    if (this.submitting() || this.cart.isEmpty()) {
      return;
    }
    if (this.isPhysical() && this.address.invalid) {
      this.address.markAllAsTouched();
      this.notifier.error('Completa la dirección de envío.');
      return;
    }
    let reference = '';
    if (this.method() === 'CARD') {
      if (this.card.invalid) {
        this.card.markAllAsTouched();
        this.notifier.error('Revisa los datos de la tarjeta.');
        return;
      }
      reference = '**** ' + this.card.controls.number.value.replace(/\s/g, '').slice(-4);
    } else if (this.method() === 'PAYPAL') {
      if (this.paypalEmail.invalid) {
        this.paypalEmail.markAsTouched();
        this.notifier.error('Ingresa un correo de PayPal válido.');
        return;
      }
      reference = this.paypalEmail.value || 'paypal';
    } else if (this.isWallet()) {
      reference = this.operation.value || 'N/A';
    }

    this.submitting.set(true);
    this.cartService
      .checkout({
        shippingAddress: this.isPhysical() ? this.address.getRawValue() : undefined,
        paymentMethod: this.method(),
        paymentReference: reference,
        deliveryType: this.delivery(),
        couponCode: this.appliedCoupon() ?? undefined,
      })
      .subscribe({
        next: () => {
          this.notifier.success('¡Compra realizada! Tu pedido se está procesando.');
          this.cart.load();
          void this.router.navigate(['/orders']);
        },
        error: (problem: ProblemDetail) => {
          this.submitting.set(false);
          if (!applyServerErrors(this.address, problem)) {
            this.notifier.error(problem.detail ?? 'No se pudo completar la compra.');
          }
        },
      });
  }
}

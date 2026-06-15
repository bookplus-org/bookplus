export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAYMENT_PROCESSING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface OrderItem {
  bookId: string;
  isbn: string;
  title: string;
  imageUrl?: string;
  unitPrice: number;
  currency: string;
  quantity: number;
  subtotal: number;
}

export interface ShippingAddress {
  recipientName: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Order {
  orderId: string;
  userId: string;
  cartId: string;
  status: OrderStatus;
  items: OrderItem[];
  total: number;
  currency: string;
  shippingAddress?: ShippingAddress;
  paymentMethod?: PaymentMethod;
  deliveryType?: DeliveryType;
  paymentId?: string;
  carrier?: string;
  trackingNumber?: string;
  deliveryCode?: string;
  receivedBy?: string;
  assignedCourier?: string;
  assignedCourierName?: string;
  claimStatus?: 'NONE' | 'OPEN' | 'RESOLVED';
  claimReason?: string;
  claimResolution?: string;
  couponCode?: string;
  discountAmount?: number;
  createdAt: string;
  updatedAt: string;
}

export type DeliveryType = 'DIGITAL' | 'PHYSICAL';

export type PaymentMethod = 'YAPE' | 'PLIN' | 'CARD' | 'CASH' | 'PAYPAL';

export const PAYMENT_METHOD_LABEL: Record<PaymentMethod, string> = {
  YAPE: 'Yape',
  PLIN: 'Plin',
  CARD: 'Tarjeta',
  CASH: 'Efectivo contra entrega',
  PAYPAL: 'PayPal',
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Pendiente de pago',
  PAYMENT_PROCESSING: 'Procesando pago',
  CONFIRMED: 'Confirmado',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
  REFUNDED: 'Reembolsado',
};

/** Estados pagados desde los que el admin puede emitir un reembolso. */
export const REFUNDABLE_STATUSES: ReadonlySet<OrderStatus> = new Set<OrderStatus>([
  'CONFIRMED',
  'SHIPPED',
  'DELIVERED',
]);

/** El cliente solo puede cancelar mientras el pago no se haya confirmado. */
export const CANCELLABLE_STATUSES: ReadonlySet<OrderStatus> = new Set<OrderStatus>([
  'PENDING_PAYMENT',
  'PAYMENT_PROCESSING',
]);

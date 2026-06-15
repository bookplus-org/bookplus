export interface CartItem {
  bookId: string;
  isbn: string;
  title: string;
  imageUrl?: string;
  unitPrice: number;
  currency: string;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  cartId?: string;
  userId?: string;
  items: CartItem[];
  itemCount: number;
  total: number;
  currency: string;
  updatedAt?: string;
}

/** cart-service requires the full denormalized item on add. */
export interface AddItemRequest {
  bookId: string;
  isbn: string;
  title: string;
  imageUrl?: string;
  unitPrice: number;
  currency: string;
  quantity: number;
}

export interface UpdateQuantityRequest {
  quantity: number;
}

export interface ShippingAddress {
  recipientName: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export type PaymentMethod = 'YAPE' | 'PLIN' | 'CARD' | 'CASH' | 'PAYPAL';

export type DeliveryType = 'DIGITAL' | 'PHYSICAL';

export interface CheckoutRequest {
  /** Only required for physical delivery. */
  shippingAddress?: ShippingAddress;
  paymentMethod: PaymentMethod;
  /** Method-specific reference: card last 4, Yape/Plin operation number, etc. */
  paymentReference?: string;
  deliveryType: DeliveryType;
  couponCode?: string;
}

export interface CouponValidation {
  valid: boolean;
  code: string | null;
  discount: number;
  finalAmount: number;
  message: string | null;
}

export const EMPTY_CART: Cart = {
  items: [],
  itemCount: 0,
  total: 0,
  currency: 'USD',
};

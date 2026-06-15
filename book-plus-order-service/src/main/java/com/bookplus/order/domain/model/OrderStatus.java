package com.bookplus.order.domain.model;

/**
 * Order lifecycle:
 *
 *  PENDING_PAYMENT ──► PAYMENT_PROCESSING ──► CONFIRMED ──► SHIPPED ──► DELIVERED
 *          │                    │
 *          └────────────────────┴──► CANCELLED
 *
 * Cancellation is allowed only from PENDING_PAYMENT or PAYMENT_PROCESSING states.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == PAYMENT_PROCESSING;
    }

    /** Estados ya pagados desde los que el admin puede emitir un reembolso. */
    public boolean isRefundable() {
        return this == CONFIRMED || this == SHIPPED || this == DELIVERED;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }
}

package com.bookplus.payment.domain.model;

/**
 * Payment lifecycle:
 *
 *  PENDING ──► PROCESSING ──► COMPLETED
 *      │            │
 *      └────────────┴──► FAILED
 *
 *  COMPLETED ──► REFUNDED (partial or full refund)
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }
}

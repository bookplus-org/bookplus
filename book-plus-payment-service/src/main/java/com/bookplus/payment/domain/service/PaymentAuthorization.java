package com.bookplus.payment.domain.service;

/**
 * Resultado de autorizar un pago en la pasarela (simulada).
 * approved=true  → transactionRef contiene la referencia de la operación.
 * approved=false → declineReason explica el rechazo.
 */
public record PaymentAuthorization(boolean approved, String transactionRef, String declineReason) {

    public static PaymentAuthorization approved(String transactionRef) {
        return new PaymentAuthorization(true, transactionRef, null);
    }

    public static PaymentAuthorization declined(String reason) {
        return new PaymentAuthorization(false, null, reason);
    }
}

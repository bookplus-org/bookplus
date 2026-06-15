package com.bookplus.payment.domain.model;

/**
 * Métodos de pago soportados.
 *
 * Los cuatro primeros son los métodos locales (simulados) ofrecidos en el
 * checkout de BookPlus. El resto se conservan por compatibilidad histórica.
 */
public enum PaymentMethod {
    YAPE,
    PLIN,
    CARD,
    CASH,
    // Legacy / compat
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    BANK_TRANSFER,
    CRYPTO
}

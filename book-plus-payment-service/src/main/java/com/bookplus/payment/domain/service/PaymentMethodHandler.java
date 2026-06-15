package com.bookplus.payment.domain.service;

import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.model.PaymentMethod;

/**
 * Strategy: cada método de pago (Yape, Plin, tarjeta, efectivo) implementa su
 * propia lógica de autorización. Añadir un método nuevo = añadir un nuevo bean
 * sin tocar el caso de uso (Open/Closed Principle).
 */
public interface PaymentMethodHandler {

    /** ¿Este handler atiende el método dado? */
    boolean supports(PaymentMethod method);

    /** Autoriza el pago contra la pasarela (simulada) y devuelve el resultado. */
    PaymentAuthorization authorize(Payment payment);
}

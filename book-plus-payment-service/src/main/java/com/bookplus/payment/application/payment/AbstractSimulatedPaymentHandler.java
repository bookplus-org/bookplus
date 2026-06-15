package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.model.PaymentMethod;
import com.bookplus.payment.domain.service.PaymentAuthorization;
import com.bookplus.payment.domain.service.PaymentMethodHandler;

import java.util.Set;
import java.util.UUID;

/**
 * Plantilla común para los handlers simulados: genera una referencia de
 * transacción con prefijo propio del método y aprueba el pago. Las subclases
 * solo declaran qué métodos soportan, su prefijo y (opcionalmente) sobrescriben
 * la decisión de autorización.
 */
public abstract class AbstractSimulatedPaymentHandler implements PaymentMethodHandler {

    /** Métodos que este handler atiende. */
    protected abstract Set<PaymentMethod> supportedMethods();

    /** Prefijo de la referencia de transacción generada (p. ej. "YAPE"). */
    protected abstract String referencePrefix();

    @Override
    public boolean supports(PaymentMethod method) {
        return supportedMethods().contains(method);
    }

    @Override
    public PaymentAuthorization authorize(Payment payment) {
        return PaymentAuthorization.approved(newReference());
    }

    /** Referencia única y legible: PREFIX-AB12CD34. */
    protected String newReference() {
        return referencePrefix() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

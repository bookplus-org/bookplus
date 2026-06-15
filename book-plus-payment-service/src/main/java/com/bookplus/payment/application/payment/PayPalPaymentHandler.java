package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Pago con PayPal (simulado): aprueba y genera una referencia PP-XXXX. */
@Component
public class PayPalPaymentHandler extends AbstractSimulatedPaymentHandler {

    @Override
    protected Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.PAYPAL);
    }

    @Override
    protected String referencePrefix() {
        return "PP";
    }
}

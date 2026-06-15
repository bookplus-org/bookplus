package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Pago con billetera Yape (simulado): aprueba y genera una referencia YAPE-XXXX. */
@Component
public class YapePaymentHandler extends AbstractSimulatedPaymentHandler {

    @Override
    protected Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.YAPE);
    }

    @Override
    protected String referencePrefix() {
        return "YAPE";
    }
}

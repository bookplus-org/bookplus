package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Pago con billetera Plin (simulado): aprueba y genera una referencia PLIN-XXXX. */
@Component
public class PlinPaymentHandler extends AbstractSimulatedPaymentHandler {

    @Override
    protected Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.PLIN);
    }

    @Override
    protected String referencePrefix() {
        return "PLIN";
    }
}

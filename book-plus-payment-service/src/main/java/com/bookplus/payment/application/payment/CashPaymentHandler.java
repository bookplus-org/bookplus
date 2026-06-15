package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Pago contra entrega / efectivo (simulado): se autoriza para confirmar la orden;
 * el cobro real ocurre al momento de la entrega. Referencia COD-XXXX.
 */
@Component
public class CashPaymentHandler extends AbstractSimulatedPaymentHandler {

    @Override
    protected Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.CASH);
    }

    @Override
    protected String referencePrefix() {
        return "COD";
    }
}

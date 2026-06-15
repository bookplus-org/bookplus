package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Pago con tarjeta de crédito/débito (simulado): aprueba y genera una
 * referencia CARD-XXXX. También atiende los métodos legacy CREDIT_CARD/DEBIT_CARD.
 */
@Component
public class CardPaymentHandler extends AbstractSimulatedPaymentHandler {

    @Override
    protected Set<PaymentMethod> supportedMethods() {
        return Set.of(PaymentMethod.CARD, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD);
    }

    @Override
    protected String referencePrefix() {
        return "CARD";
    }
}

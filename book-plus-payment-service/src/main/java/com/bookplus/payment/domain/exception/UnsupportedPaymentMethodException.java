package com.bookplus.payment.domain.exception;

import com.bookplus.payment.domain.model.PaymentMethod;

public class UnsupportedPaymentMethodException extends DomainException {
    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("No payment handler registered for method: " + method);
    }
}

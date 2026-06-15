package com.bookplus.payment.domain.exception;
import com.bookplus.payment.domain.model.PaymentStatus;
public class InvalidPaymentTransitionException extends DomainException {
    public InvalidPaymentTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Invalid payment transition: " + from + " → " + to);
    }
}

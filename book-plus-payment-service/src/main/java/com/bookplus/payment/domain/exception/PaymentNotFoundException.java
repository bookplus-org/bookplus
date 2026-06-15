package com.bookplus.payment.domain.exception;
public class PaymentNotFoundException extends DomainException {
    public PaymentNotFoundException(String id) { super("Payment not found: " + id); }
}

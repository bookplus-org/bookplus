package com.bookplus.payment.domain.exception;
public class PaymentAlreadyExistsException extends DomainException {
    public PaymentAlreadyExistsException(String orderId) {
        super("A payment already exists for order: " + orderId);
    }
}

package com.bookplus.order.domain.exception;

public class DuplicateCouponException extends DomainException {
    public DuplicateCouponException(String code) {
        super("Ya existe un cupón con ese código: " + code);
    }
}

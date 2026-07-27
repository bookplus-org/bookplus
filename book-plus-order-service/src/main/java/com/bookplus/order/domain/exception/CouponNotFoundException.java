package com.bookplus.order.domain.exception;

public class CouponNotFoundException extends DomainException {
    public CouponNotFoundException(String code) {
        super("Coupon not found: " + code);
    }
}

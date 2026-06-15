package com.bookplus.cart.domain.exception;

public class CartNotFoundException extends DomainException {
    public CartNotFoundException(String userId) {
        super("Cart not found for user: " + userId);
    }
}

package com.bookplus.order.domain.exception;

import com.bookplus.order.domain.model.OrderStatus;

public class InvalidOrderTransitionException extends DomainException {
    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order status transition: " + from + " → " + to);
    }
}

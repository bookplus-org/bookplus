package com.bookplus.order.domain.exception;

import com.bookplus.order.domain.model.OrderStatus;

public class OrderNotCancellableException extends DomainException {
    public OrderNotCancellableException(String orderId, OrderStatus currentStatus) {
        super("Order " + orderId + " cannot be cancelled in status " + currentStatus);
    }
}

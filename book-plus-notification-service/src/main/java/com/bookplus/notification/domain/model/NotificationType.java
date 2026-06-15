package com.bookplus.notification.domain.model;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_REFUNDED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    LOW_STOCK_ALERT,    // for internal team
    REVIEW_ADDED        // optional: notify author / admin
}

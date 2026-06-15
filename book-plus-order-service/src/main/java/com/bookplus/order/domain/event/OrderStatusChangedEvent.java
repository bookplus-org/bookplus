package com.bookplus.order.domain.event;

import com.bookplus.order.domain.model.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Generic status transition event.
 * Consumed by: notification-service to notify the customer of shipping/delivery updates.
 */
@Getter
public class OrderStatusChangedEvent implements DomainEvent {

    private final String      orderId;
    private final String      userId;
    private final String      recipientEmail;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final Instant     occurredOn;

    public OrderStatusChangedEvent(String orderId, String userId, String recipientEmail,
                                   OrderStatus previousStatus, OrderStatus newStatus) {
        this.orderId        = orderId;
        this.userId         = userId;
        this.recipientEmail = recipientEmail;
        this.previousStatus = previousStatus;
        this.newStatus      = newStatus;
        this.occurredOn     = Instant.now();
    }

    @Override public String  aggregateId() { return orderId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

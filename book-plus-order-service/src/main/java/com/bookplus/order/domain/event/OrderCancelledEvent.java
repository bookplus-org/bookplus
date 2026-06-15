package com.bookplus.order.domain.event;

import com.bookplus.order.domain.model.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Published when an order is cancelled.
 * Consumed by: inventory-service (release reservation), payment-service (refund if paid),
 *              notification-service (cancellation email).
 */
@Getter
public class OrderCancelledEvent implements DomainEvent {

    private final String     orderId;
    private final String     userId;
    private final String     reason;
    private final List<Item> items;
    private final Instant    occurredOn;

    public record Item(String bookId, int quantity) {}

    public OrderCancelledEvent(String orderId, String userId, String reason, List<Item> items) {
        this.orderId    = orderId;
        this.userId     = userId;
        this.reason     = reason;
        this.items      = List.copyOf(items);
        this.occurredOn = Instant.now();
    }

    @Override public String  aggregateId() { return orderId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

package com.bookplus.order.domain.event;

import com.bookplus.order.domain.model.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Published when an order is refunded AND the admin chose to return the items to stock.
 * Consumed by: inventory-service (add stock back for the refunded quantities).
 */
@Getter
public class OrderRefundedEvent implements DomainEvent {

    private final String     orderId;
    private final String     userId;
    private final String     reason;
    private final List<Item> items;
    private final Instant    occurredOn;

    public record Item(String bookId, int quantity) {}

    public OrderRefundedEvent(String orderId, String userId, String reason, List<Item> items) {
        this.orderId    = orderId;
        this.userId     = userId;
        this.reason     = reason;
        this.items      = List.copyOf(items);
        this.occurredOn = Instant.now();
    }

    @Override public String  aggregateId() { return orderId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

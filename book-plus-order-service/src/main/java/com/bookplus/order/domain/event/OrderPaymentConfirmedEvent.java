package com.bookplus.order.domain.event;

import com.bookplus.order.domain.model.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Published when an order's payment is confirmed (order → CONFIRMED).
 * Consumed by inventory-service to confirm the stock reservation (definitive
 * deduction) for each line item.
 *
 * Kafka topic: order.payment.confirmed
 */
@Getter
public class OrderPaymentConfirmedEvent implements DomainEvent {

    private final String     orderId;
    private final String     userId;
    private final List<Item> items;
    private final String     deliveryType;   // DIGITAL | PHYSICAL
    private final Instant    occurredOn;

    public record Item(String bookId, int quantity) {}

    public OrderPaymentConfirmedEvent(String orderId, String userId, List<Item> items, String deliveryType) {
        this.orderId      = orderId;
        this.userId       = userId;
        this.items        = List.copyOf(items);
        this.deliveryType = deliveryType;
        this.occurredOn   = Instant.now();
    }

    @Override public String  aggregateId() { return orderId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

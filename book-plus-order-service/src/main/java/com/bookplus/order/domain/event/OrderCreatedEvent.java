package com.bookplus.order.domain.event;

import com.bookplus.order.domain.model.*;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Published when a new order is created from a CartCheckedOutEvent.
 * Consumed by: inventory-service (reserve stock), payment-service (initiate payment),
 *              notification-service (order confirmation email).
 */
@Getter
public class OrderCreatedEvent implements DomainEvent {

    private final String      orderId;
    private final String      userId;
    private final String      recipientEmail;
    private final List<Item>  items;
    private final Money       total;
    private final String      paymentMethod;
    private final Instant     occurredOn;

    public record Item(String bookId, String isbn, String title, int quantity, Money unitPrice) {}

    public OrderCreatedEvent(String orderId, String userId, String recipientEmail,
                             List<Item> items, Money total, String paymentMethod) {
        this.orderId        = orderId;
        this.userId         = userId;
        this.recipientEmail = recipientEmail;
        this.items          = List.copyOf(items);
        this.total          = total;
        this.paymentMethod  = paymentMethod;
        this.occurredOn     = Instant.now();
    }

    @Override public String  aggregateId() { return orderId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

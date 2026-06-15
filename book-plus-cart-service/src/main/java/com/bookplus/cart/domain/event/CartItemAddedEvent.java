package com.bookplus.cart.domain.event;

import com.bookplus.cart.domain.model.*;
import java.time.Instant;
import java.util.UUID;

/** Kafka topic: cart.item.added */
public record CartItemAddedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        CartId cartId, String userId, BookId bookId, int quantity, Money unitPrice
) implements DomainEvent {
    public CartItemAddedEvent(CartId cartId, String userId,
                               BookId bookId, int quantity, Money unitPrice) {
        this(UUID.randomUUID(), "CART_ITEM_ADDED", Instant.now(),
             cartId, userId, bookId, quantity, unitPrice);
    }
}

package com.bookplus.cart.domain.event;

import com.bookplus.cart.domain.model.*;
import java.time.Instant;
import java.util.UUID;

/** Kafka topic: cart.item.removed */
public record CartItemRemovedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        CartId cartId, String userId, BookId bookId
) implements DomainEvent {
    public CartItemRemovedEvent(CartId cartId, String userId, BookId bookId) {
        this(UUID.randomUUID(), "CART_ITEM_REMOVED", Instant.now(), cartId, userId, bookId);
    }
}

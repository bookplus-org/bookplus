package com.bookplus.cart.domain.event;

import com.bookplus.cart.domain.model.CartId;
import java.time.Instant;
import java.util.UUID;

/** Kafka topic: cart.cleared */
public record CartClearedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        CartId cartId, String userId
) implements DomainEvent {
    public CartClearedEvent(CartId cartId, String userId) {
        this(UUID.randomUUID(), "CART_CLEARED", Instant.now(), cartId, userId);
    }
}

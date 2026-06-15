package com.bookplus.inventory.domain.event;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.ReservationId;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: inventory.stock.reserved — consumido por order-service */
public record StockReservedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        ReservationId reservationId, BookId bookId,
        String orderId, String userId, int quantity
) implements DomainEvent {
    public StockReservedEvent(ReservationId reservationId, BookId bookId,
                               String orderId, String userId, int quantity) {
        this(UUID.randomUUID(), "STOCK_RESERVED", Instant.now(),
             reservationId, bookId, orderId, userId, quantity);
    }
}

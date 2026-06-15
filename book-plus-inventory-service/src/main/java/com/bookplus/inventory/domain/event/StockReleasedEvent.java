package com.bookplus.inventory.domain.event;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.ReservationId;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: inventory.stock.released */
public record StockReleasedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        ReservationId reservationId, BookId bookId,
        String orderId, int quantity, String reason
) implements DomainEvent {
    public StockReleasedEvent(ReservationId reservationId, BookId bookId,
                               String orderId, int quantity, String reason) {
        this(UUID.randomUUID(), "STOCK_RELEASED", Instant.now(),
             reservationId, bookId, orderId, quantity, reason);
    }
}

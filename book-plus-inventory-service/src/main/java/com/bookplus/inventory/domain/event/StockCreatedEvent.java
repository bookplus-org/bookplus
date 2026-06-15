package com.bookplus.inventory.domain.event;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockId;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: inventory.stock.created */
public record StockCreatedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        StockId stockId, BookId bookId, int initialQuantity
) implements DomainEvent {
    public StockCreatedEvent(StockId stockId, BookId bookId, int initialQuantity) {
        this(UUID.randomUUID(), "STOCK_CREATED", Instant.now(), stockId, bookId, initialQuantity);
    }
}

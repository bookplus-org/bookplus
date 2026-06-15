package com.bookplus.inventory.domain.event;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockId;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka topic: inventory.stock.updated
 *
 * Consumido por catalog-service para actualizar el stockSnapshot del libro.
 */
public record StockUpdatedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        StockId stockId, BookId bookId,
        int quantityAvailable, int quantityReserved
) implements DomainEvent {
    public StockUpdatedEvent(StockId stockId, BookId bookId,
                              int quantityAvailable, int quantityReserved) {
        this(UUID.randomUUID(), "STOCK_UPDATED", Instant.now(),
             stockId, bookId, quantityAvailable, quantityReserved);
    }
}

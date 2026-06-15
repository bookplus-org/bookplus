package com.bookplus.inventory.domain.event;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockId;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: inventory.stock.low-alert — consumido por notification-service */
public record LowStockAlertEvent(
        UUID eventId, String eventType, Instant occurredOn,
        StockId stockId, BookId bookId,
        int currentAvailable, int threshold
) implements DomainEvent {
    public LowStockAlertEvent(StockId stockId, BookId bookId,
                               int currentAvailable, int threshold) {
        this(UUID.randomUUID(), "LOW_STOCK_ALERT", Instant.now(),
             stockId, bookId, currentAvailable, threshold);
    }
}

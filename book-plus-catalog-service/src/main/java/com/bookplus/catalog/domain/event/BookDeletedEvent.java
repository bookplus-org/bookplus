package com.bookplus.catalog.domain.event;

import com.bookplus.catalog.domain.model.*;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: catalog.book.deleted */
public record BookDeletedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        BookId bookId, ISBN isbn
) implements DomainEvent {
    public BookDeletedEvent(BookId bookId, ISBN isbn) {
        this(UUID.randomUUID(), "BOOK_DELETED", Instant.now(), bookId, isbn);
    }
}

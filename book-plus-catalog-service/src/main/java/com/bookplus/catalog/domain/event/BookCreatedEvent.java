package com.bookplus.catalog.domain.event;

import com.bookplus.catalog.domain.model.*;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: catalog.book.created */
public record BookCreatedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        BookId bookId, ISBN isbn, String title, String author,
        CategoryId categoryId, Money price
) implements DomainEvent {
    public BookCreatedEvent(BookId bookId, ISBN isbn, String title,
                            String author, CategoryId categoryId, Money price) {
        this(UUID.randomUUID(), "BOOK_CREATED", Instant.now(),
             bookId, isbn, title, author, categoryId, price);
    }
}

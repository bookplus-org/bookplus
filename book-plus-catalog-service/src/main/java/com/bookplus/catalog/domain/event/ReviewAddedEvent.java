package com.bookplus.catalog.domain.event;

import com.bookplus.catalog.domain.model.*;

import java.time.Instant;
import java.util.UUID;

/** Kafka topic: catalog.review.added */
public record ReviewAddedEvent(
        UUID eventId, String eventType, Instant occurredOn,
        ReviewId reviewId, BookId bookId, String userId,
        Rating rating, boolean verifiedPurchase
) implements DomainEvent {
    public ReviewAddedEvent(ReviewId reviewId, BookId bookId,
                             String userId, Rating rating, boolean verifiedPurchase) {
        this(UUID.randomUUID(), "REVIEW_ADDED", Instant.now(),
             reviewId, bookId, userId, rating, verifiedPurchase);
    }
}

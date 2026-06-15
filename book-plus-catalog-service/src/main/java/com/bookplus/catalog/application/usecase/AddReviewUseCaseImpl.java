package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.event.ReviewAddedEvent;
import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.AddReviewUseCase;
import com.bookplus.catalog.domain.port.out.*;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AddReviewUseCaseImpl implements AddReviewUseCase {

    private final LoadBookPort            loadBookPort;
    private final SaveBookPort            saveBookPort;
    private final LoadReviewPort          loadReviewPort;
    private final SaveReviewPort          saveReviewPort;
    private final DomainEventPublisherPort eventPublisher;
    private final CachePort               cachePort;

    @Override
    public Review add(AddReviewCommand command) {
        log.debug("Adding review: bookId='{}' userId='{}'", command.bookId(), command.userId());

        BookId bookId = BookId.of(command.bookId());

        // 1. Validate book exists and is active
        Book book = loadBookPort.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(command.bookId()));

        if (!book.isActive()) {
            throw new com.bookplus.catalog.domain.exception.DomainException(
                    "Cannot review an inactive book: " + command.bookId());
        }

        // 2. One review per user per book
        if (loadReviewPort.existsByBookIdAndUserId(bookId, command.userId())) {
            throw new com.bookplus.catalog.domain.exception.DomainException(
                    "User already reviewed this book: userId=" + command.userId());
        }

        // 3. Create the review entity
        Rating rating = Rating.of(command.rating());
        Review review = Review.create(
                bookId,
                command.userId(),
                command.username(),
                rating,
                command.comment(),
                command.verifiedPurchase()
        );

        // 4. Persist review
        Review saved = saveReviewPort.save(review);

        // 5. Update aggregate stats — recalculate average rating
        //    We load fresh stats from the review port (count, avg) and update the book aggregate.
        //    Using a simplified approach: add the new rating to the book aggregate stats.
        book.addReviewStats(rating);
        saveBookPort.save(book);

        // 6. Evict the book from cache (updated rating)
        try {
            cachePort.evictBook(bookId.value().toString());
        } catch (Exception ex) {
            log.warn("Cache eviction failed for book {}: {}", bookId.value(), ex.getMessage());
        }

        // 7. Publish domain event
        ReviewAddedEvent event = new ReviewAddedEvent(
                saved.getId(), bookId,
                command.userId(), rating,
                command.verifiedPurchase()
        );
        try {
            eventPublisher.publish(event);
        } catch (Exception ex) {
            log.error("Failed to publish ReviewAddedEvent for book {}: {}", bookId.value(), ex.getMessage());
        }

        log.info("Review added: reviewId={} bookId={} userId={}",
                saved.getId().value(), bookId.value(), command.userId());
        return saved;
    }
}

package com.bookplus.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity — Reseña de un libro.
 * Pertenece al agregado Book pero se persiste independientemente.
 */
public class Review {

    private final ReviewId  id;
    private final BookId    bookId;
    private final String    userId;      // referencia al auth-service (desnormalizado)
    private final String    username;    // desnormalizado para evitar join cross-service
    private final Rating    rating;
    private final String    comment;
    private final boolean   verifiedPurchase;
    private final Instant   createdAt;

    private Review(ReviewId id, BookId bookId, String userId, String username,
                   Rating rating, String comment, boolean verifiedPurchase, Instant createdAt) {
        this.id               = Objects.requireNonNull(id);
        this.bookId           = Objects.requireNonNull(bookId);
        this.userId           = Objects.requireNonNull(userId);
        this.username         = Objects.requireNonNull(username);
        this.rating           = Objects.requireNonNull(rating);
        this.comment          = comment;
        this.verifiedPurchase = verifiedPurchase;
        this.createdAt        = Objects.requireNonNull(createdAt);
    }

    public static Review create(BookId bookId, String userId, String username,
                                Rating rating, String comment, boolean verifiedPurchase) {
        if (comment != null && comment.length() > 2000) {
            throw new com.bookplus.catalog.domain.exception.DomainException(
                    "Review comment must not exceed 2000 characters");
        }
        return new Review(ReviewId.generate(), bookId, userId, username,
                rating, comment, verifiedPurchase, Instant.now());
    }

    public static Review reconstitute(ReviewId id, BookId bookId, String userId, String username,
                                      Rating rating, String comment,
                                      boolean verifiedPurchase, Instant createdAt) {
        return new Review(id, bookId, userId, username, rating, comment, verifiedPurchase, createdAt);
    }

    public ReviewId  getId()              { return id; }
    public BookId    getBookId()          { return bookId; }
    public String    getUserId()          { return userId; }
    public String    getUsername()        { return username; }
    public Rating    getRating()          { return rating; }
    public String    getComment()         { return comment; }
    public boolean   isVerifiedPurchase() { return verifiedPurchase; }
    public Instant   getCreatedAt()       { return createdAt; }
}

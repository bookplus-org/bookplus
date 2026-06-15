package com.bookplus.catalog.adapter.in.web.dto;

import com.bookplus.catalog.domain.model.Review;

import java.time.Instant;

public record ReviewResponse(
        String  id,
        String  bookId,
        String  userId,
        String  username,
        int     rating,
        String  comment,
        boolean verifiedPurchase,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId().value().toString(),
                r.getBookId().value().toString(),
                r.getUserId(),
                r.getUsername(),
                r.getRating().value(),
                r.getComment(),
                r.isVerifiedPurchase(),
                r.getCreatedAt()
        );
    }
}

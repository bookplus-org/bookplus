package com.bookplus.catalog.adapter.out.persistence.mapper;

import com.bookplus.catalog.adapter.out.persistence.entity.ReviewEntity;
import com.bookplus.catalog.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class ReviewPersistenceMapper {

    public Review toDomain(ReviewEntity e) {
        return Review.reconstitute(
                ReviewId.of(e.getId()),
                BookId.of(e.getBookId()),
                e.getUserId(),
                e.getUsername(),
                Rating.of(e.getRating()),
                e.getComment(),
                e.isVerifiedPurchase(),
                e.getCreatedAt()
        );
    }

    public ReviewEntity toEntity(Review r) {
        return ReviewEntity.builder()
                .id(r.getId().value())
                .bookId(r.getBookId().value())
                .userId(r.getUserId())
                .username(r.getUsername())
                .rating(r.getRating().value())
                .comment(r.getComment())
                .verifiedPurchase(r.isVerifiedPurchase())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

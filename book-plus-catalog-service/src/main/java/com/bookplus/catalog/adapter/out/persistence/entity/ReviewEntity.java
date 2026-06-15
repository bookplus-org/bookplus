package com.bookplus.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_reviews_book_user",
            columnNames = {"book_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_reviews_book_id",    columnList = "book_id"),
        @Index(name = "idx_reviews_user_id",    columnList = "user_id"),
        @Index(name = "idx_reviews_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private UUID bookId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "verified_purchase", nullable = false)
    private boolean verifiedPurchase;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

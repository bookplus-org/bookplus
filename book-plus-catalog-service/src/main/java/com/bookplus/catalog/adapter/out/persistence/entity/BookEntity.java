package com.bookplus.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_books_isbn",        columnList = "isbn",        unique = true),
        @Index(name = "idx_books_slug",        columnList = "slug",        unique = true),
        @Index(name = "idx_books_category_id", columnList = "category_id"),
        @Index(name = "idx_books_author",      columnList = "author"),
        @Index(name = "idx_books_active",      columnList = "active"),
        @Index(name = "idx_books_published",   columnList = "published_date")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "preview_url", length = 512)
    private String previewUrl;

    @Column(length = 200)
    private String publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(length = 10)
    private String language;

    private Integer pages;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "stock_snapshot", nullable = false)
    private int stockSnapshot = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

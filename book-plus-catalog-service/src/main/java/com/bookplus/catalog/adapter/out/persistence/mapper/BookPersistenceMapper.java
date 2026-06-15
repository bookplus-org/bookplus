package com.bookplus.catalog.adapter.out.persistence.mapper;

import com.bookplus.catalog.adapter.out.persistence.entity.BookEntity;
import com.bookplus.catalog.domain.model.*;
import org.springframework.stereotype.Component;

/**
 * Mapper between Book domain aggregate and BookEntity JPA entity.
 * Keeps domain and persistence model fully decoupled.
 */
@Component
public class BookPersistenceMapper {

    public Book toDomain(BookEntity e) {
        return Book.reconstitute(
                BookId.of(e.getId()),
                ISBN.of(e.getIsbn()),
                e.getTitle(),
                Slug.of(e.getSlug()),
                e.getAuthor(),
                e.getDescription(),
                Money.of(e.getPrice(), e.getCurrency()),
                e.getDiscountPrice() != null ? Money.of(e.getDiscountPrice(), e.getCurrency()) : null,
                e.getImageUrl(),
                e.getPreviewUrl(),
                e.getPublisher(),
                e.getPublishedDate(),
                e.getLanguage(),
                e.getPages(),
                CategoryId.of(e.getCategoryId()),
                e.isActive(),
                e.getStockSnapshot(),
                e.getAverageRating(),
                e.getReviewCount(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public BookEntity toEntity(Book b) {
        return BookEntity.builder()
                .id(b.getId().value())
                .isbn(b.getIsbn().value())
                .title(b.getTitle())
                .slug(b.getSlug().value())
                .author(b.getAuthor())
                .description(b.getDescription())
                .price(b.getPrice().amount())
                .currency(b.getPrice().currency())
                .discountPrice(b.getDiscountPrice() != null ? b.getDiscountPrice().amount() : null)
                .imageUrl(b.getImageUrl())
                .previewUrl(b.getPreviewUrl())
                .publisher(b.getPublisher())
                .publishedDate(b.getPublishedDate())
                .language(b.getLanguage())
                .pages(b.getPages())
                .categoryId(b.getCategoryId().value())
                .active(b.isActive())
                .stockSnapshot(b.getStockSnapshot())
                .averageRating(b.getAverageRating())
                .reviewCount(b.getReviewCount())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

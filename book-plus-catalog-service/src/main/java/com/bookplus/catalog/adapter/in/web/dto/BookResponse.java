package com.bookplus.catalog.adapter.in.web.dto;

import com.bookplus.catalog.domain.model.Book;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** DTO de respuesta completo para un libro (detalle). */
public record BookResponse(
        String      id,
        String      isbn,
        String      title,
        String      slug,
        String      author,
        String      description,
        BigDecimal  price,
        String      currency,
        BigDecimal  discountPrice,
        boolean     hasDiscount,
        String      imageUrl,
        String      previewUrl,
        String      publisher,
        LocalDate   publishedDate,
        String      language,
        Integer     pages,
        String      categoryId,
        boolean     active,
        boolean     inStock,
        int         stockSnapshot,
        BigDecimal  averageRating,
        int         reviewCount,
        Instant     createdAt,
        Instant     updatedAt
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId().value().toString(),
                book.getIsbn().value(),
                book.getTitle(),
                book.getSlug().value(),
                book.getAuthor(),
                book.getDescription(),
                book.getPrice().amount(),
                book.getPrice().currency(),
                book.getDiscountPrice() != null ? book.getDiscountPrice().amount() : null,
                book.hasDiscount(),
                book.getImageUrl(),
                book.getPreviewUrl(),
                book.getPublisher(),
                book.getPublishedDate(),
                book.getLanguage(),
                book.getPages(),
                book.getCategoryId().value().toString(),
                book.isActive(),
                book.isInStock(),
                book.getStockSnapshot(),
                book.getAverageRating(),
                book.getReviewCount(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}

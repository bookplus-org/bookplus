package com.bookplus.catalog.adapter.in.web.dto;

import com.bookplus.catalog.domain.model.Book;

import java.math.BigDecimal;

/** DTO ligero para listados y búsquedas. */
public record BookSummaryResponse(
        String     id,
        String     isbn,
        String     title,
        String     slug,
        String     author,
        String     imageUrl,
        BigDecimal price,
        String     currency,
        BigDecimal discountPrice,
        boolean    hasDiscount,
        boolean    inStock,
        BigDecimal averageRating,
        int        reviewCount,
        String     categoryId
) {
    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(
                book.getId().value().toString(),
                book.getIsbn().value(),
                book.getTitle(),
                book.getSlug().value(),
                book.getAuthor(),
                book.getImageUrl(),
                book.getPrice().amount(),
                book.getPrice().currency(),
                book.getDiscountPrice() != null ? book.getDiscountPrice().amount() : null,
                book.hasDiscount(),
                book.isInStock(),
                book.getAverageRating(),
                book.getReviewCount(),
                book.getCategoryId().value().toString()
        );
    }
}

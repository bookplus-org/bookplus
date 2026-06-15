package com.bookplus.catalog.adapter.out.search;

import com.bookplus.catalog.domain.model.Book;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Elasticsearch document — representa un libro indexado.
 * Índice: catalog-books
 */
@Document(indexName = "catalog-books", createIndex = true)
@Setting(settingPath = "elasticsearch/book-settings.json")
public record BookDocument(

        @Id
        String id,

        @Field(type = FieldType.Keyword)
        String isbn,

        @Field(type = FieldType.Text, analyzer = "standard")
        String title,

        @Field(type = FieldType.Keyword)
        String slug,

        @Field(type = FieldType.Text, analyzer = "standard")
        String author,

        @Field(type = FieldType.Text, analyzer = "standard")
        String description,

        @Field(type = FieldType.Double)
        BigDecimal price,

        @Field(type = FieldType.Keyword)
        String currency,

        @Field(type = FieldType.Double)
        BigDecimal discountPrice,

        @Field(type = FieldType.Keyword)
        String imageUrl,

        @Field(type = FieldType.Keyword)
        String publisher,

        @Field(type = FieldType.Date, format = DateFormat.year_month_day)
        LocalDate publishedDate,

        @Field(type = FieldType.Keyword)
        String language,

        @Field(type = FieldType.Integer)
        Integer pages,

        @Field(type = FieldType.Keyword)
        String categoryId,

        @Field(type = FieldType.Boolean)
        boolean active,

        @Field(type = FieldType.Integer)
        int stockSnapshot,

        @Field(type = FieldType.Double)
        BigDecimal averageRating,

        @Field(type = FieldType.Integer)
        int reviewCount,

        @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
        Instant createdAt
) {
    /** Build document from domain aggregate. */
    public static BookDocument from(Book book) {
        return new BookDocument(
                book.getId().value().toString(),
                book.getIsbn().value(),
                book.getTitle(),
                book.getSlug().value(),
                book.getAuthor(),
                book.getDescription(),
                book.getPrice().amount(),
                book.getPrice().currency(),
                book.getDiscountPrice() != null ? book.getDiscountPrice().amount() : null,
                book.getImageUrl(),
                book.getPublisher(),
                book.getPublishedDate(),
                book.getLanguage(),
                book.getPages(),
                book.getCategoryId().value().toString(),
                book.isActive(),
                book.getStockSnapshot(),
                book.getAverageRating(),
                book.getReviewCount(),
                book.getCreatedAt()
        );
    }
}

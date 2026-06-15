package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Puerto de entrada — actualizar libro (ADMIN/EDITOR). */
public interface UpdateBookUseCase {
    Book update(String bookId, UpdateBookCommand command);

    record UpdateBookCommand(
            String title, String author, String description,
            BigDecimal price, String currency, BigDecimal discountPrice,
            String imageUrl, String previewUrl, String publisher, LocalDate publishedDate,
            String language, Integer pages, String categoryId
    ) {}
}

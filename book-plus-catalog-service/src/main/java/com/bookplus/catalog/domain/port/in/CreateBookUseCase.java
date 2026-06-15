package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Puerto de entrada — crear un nuevo libro (ADMIN). */
public interface CreateBookUseCase {

    Book create(CreateBookCommand command);

    record CreateBookCommand(
            @NotBlank String isbn,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String author,
            String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank String currency,
            BigDecimal discountPrice,
            String imageUrl,
            String previewUrl,
            String publisher,
            LocalDate publishedDate,
            String language,
            Integer pages,
            @NotBlank String categoryId
    ) {}
}

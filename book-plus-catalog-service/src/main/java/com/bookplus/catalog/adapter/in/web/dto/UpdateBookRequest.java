package com.bookplus.catalog.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ignora campos extra (p. ej. isbn, que no se edita) en vez de fallar con 500. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateBookRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 255)
        String author,

        @Size(max = 5000)
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        BigDecimal discountPrice,
        String     imageUrl,
        String     previewUrl,
        String     publisher,
        LocalDate  publishedDate,
        String     language,

        @Min(1)
        Integer pages,

        @NotBlank(message = "Category ID is required")
        String categoryId
) {}

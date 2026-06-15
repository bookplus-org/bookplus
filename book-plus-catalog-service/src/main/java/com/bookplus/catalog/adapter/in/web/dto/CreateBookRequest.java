package com.bookplus.catalog.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBookRequest(
        @NotBlank(message = "ISBN is required")
        String isbn,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 255, message = "Author must not exceed 255 characters")
        String author,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Price must be positive")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        String imageUrl,
        String previewUrl,
        String publisher,
        LocalDate publishedDate,

        @Size(max = 10, message = "Language code must not exceed 10 characters")
        String language,

        @Min(value = 1, message = "Pages must be at least 1")
        Integer pages,

        @NotBlank(message = "Category ID is required")
        String categoryId
) {}

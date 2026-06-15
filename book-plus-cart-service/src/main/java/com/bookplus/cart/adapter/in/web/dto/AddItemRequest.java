package com.bookplus.cart.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AddItemRequest(
        @NotBlank(message = "bookId is required")
        String bookId,

        @NotBlank(message = "isbn is required")
        String isbn,

        @NotBlank(message = "title is required")
        String title,

        String imageUrl,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
        BigDecimal unitPrice,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 99, message = "quantity cannot exceed 99")
        int quantity
) {}

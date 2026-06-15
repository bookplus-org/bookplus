package com.bookplus.inventory.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitializeStockRequest(
        @NotBlank(message = "bookId is required")
        String bookId,

        @NotNull @Min(value = 0, message = "Initial quantity must be non-negative")
        Integer initialQuantity,

        @Min(value = 0)
        int lowStockThreshold
) {}

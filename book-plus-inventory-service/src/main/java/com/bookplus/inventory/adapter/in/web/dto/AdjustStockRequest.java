package com.bookplus.inventory.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        @NotNull @Min(value = 0, message = "New total quantity must be non-negative")
        Integer newTotalQuantity,
        @Min(value = 0)
        int lowStockThreshold,
        String notes
) {}

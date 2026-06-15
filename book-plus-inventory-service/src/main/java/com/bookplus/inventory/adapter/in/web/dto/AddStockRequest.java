package com.bookplus.inventory.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddStockRequest(
        @NotNull @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        String referenceId,
        String notes
) {}

package com.bookplus.cart.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record UpdateQuantityRequest(
        @Min(value = 0, message = "quantity must be 0 or greater (0 removes the item)")
        @Max(value = 99, message = "quantity cannot exceed 99")
        int quantity
) {}

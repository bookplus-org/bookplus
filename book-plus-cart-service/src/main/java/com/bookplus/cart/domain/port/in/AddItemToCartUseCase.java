package com.bookplus.cart.domain.port.in;

import com.bookplus.cart.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public interface AddItemToCartUseCase {

    Cart addItem(@Valid AddItemCommand command);

    record AddItemCommand(
            @NotBlank(message = "userId is required")
            String userId,

            @NotBlank(message = "bookId is required")
            String bookId,

            @NotBlank(message = "isbn is required")
            String isbn,

            @NotBlank(message = "title is required")
            String title,

            String imageUrl,

            @NotNull(message = "unitPrice is required")
            Money unitPrice,

            @Min(value = 1, message = "quantity must be at least 1")
            @Max(value = 99, message = "quantity cannot exceed 99")
            int quantity
    ) {}
}

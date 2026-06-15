package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public interface CreateOrderUseCase {

    Order createOrder(@Valid CreateOrderCommand command);

    record CreateOrderCommand(
            @NotBlank(message = "userId is required")
            String userId,

            String userEmail,

            @NotBlank(message = "cartId is required")
            String cartId,

            @NotEmpty(message = "Order must have at least one item")
            @Valid
            List<ItemDto> items,

            @NotNull(message = "total is required")
            @DecimalMin(value = "0.01", message = "total must be positive")
            BigDecimal total,

            @NotBlank(message = "currency is required")
            @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
            String currency,

            @NotNull(message = "shippingAddress is required")
            ShippingAddress shippingAddress,

            @NotBlank(message = "paymentMethod is required")
            String paymentMethod,

            @NotBlank(message = "deliveryType is required")
            String deliveryType,

            String couponCode,
            java.math.BigDecimal discountAmount
    ) {
        public record ItemDto(
                @NotBlank(message = "bookId is required")
                String bookId,

                @NotBlank(message = "isbn is required")
                String isbn,

                @NotBlank(message = "title is required")
                String title,

                String imageUrl,

                @NotNull(message = "unitPrice is required")
                @DecimalMin(value = "0.01", message = "unitPrice must be positive")
                BigDecimal unitPrice,

                @NotBlank(message = "currency is required")
                String currency,

                @Min(value = 1, message = "quantity must be at least 1")
                @Max(value = 999, message = "quantity cannot exceed 999")
                int quantity
        ) {}
    }
}

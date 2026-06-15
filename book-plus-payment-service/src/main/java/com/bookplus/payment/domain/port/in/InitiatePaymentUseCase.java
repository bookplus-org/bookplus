package com.bookplus.payment.domain.port.in;

import com.bookplus.payment.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public interface InitiatePaymentUseCase {

    Payment initiate(@Valid InitiatePaymentCommand command);

    record InitiatePaymentCommand(
            @NotBlank(message = "orderId is required")
            String orderId,

            @NotBlank(message = "userId is required")
            String userId,

            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be positive")
            BigDecimal amount,

            @NotBlank(message = "currency is required")
            @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
            String currency,

            @NotNull(message = "paymentMethod is required")
            PaymentMethod paymentMethod
    ) {}
}

package com.bookplus.payment.domain.port.in;

import com.bookplus.payment.domain.model.Payment;

public interface ProcessPaymentUseCase {
    /** Simulates gateway webhook: payment was approved */
    Payment complete(String paymentId, String transactionRef);

    /** Simulates gateway webhook: payment was rejected */
    Payment fail(String paymentId, String reason);
}

package com.bookplus.payment.domain.port.in;

import com.bookplus.payment.domain.model.Payment;

public interface RefundPaymentUseCase {
    Payment refund(String orderId, String reason);
}

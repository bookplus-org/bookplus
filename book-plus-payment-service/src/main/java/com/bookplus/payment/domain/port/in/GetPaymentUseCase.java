package com.bookplus.payment.domain.port.in;

import com.bookplus.payment.domain.model.Payment;

public interface GetPaymentUseCase {
    Payment getByPaymentId(String paymentId);
    Payment getByOrderId(String orderId);
}

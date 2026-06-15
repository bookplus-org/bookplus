package com.bookplus.payment.domain.port.out;

import com.bookplus.payment.domain.model.Payment;
import java.util.Optional;

public interface LoadPaymentPort {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByOrderId(String orderId);
}

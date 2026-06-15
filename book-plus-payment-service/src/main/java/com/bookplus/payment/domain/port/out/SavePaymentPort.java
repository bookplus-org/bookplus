package com.bookplus.payment.domain.port.out;

import com.bookplus.payment.domain.model.Payment;

public interface SavePaymentPort {
    Payment save(Payment payment);
}

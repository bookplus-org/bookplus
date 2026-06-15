package com.bookplus.payment.application.usecase;

import com.bookplus.payment.domain.exception.PaymentNotFoundException;
import com.bookplus.payment.domain.model.Payment;
import com.bookplus.payment.domain.port.in.GetPaymentUseCase;
import com.bookplus.payment.domain.port.out.LoadPaymentPort;
import com.bookplus.payment.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase @RequiredArgsConstructor
public class GetPaymentUseCaseImpl implements GetPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;

    @Override
    public Payment getByPaymentId(String paymentId) {
        return loadPaymentPort.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Override
    public Payment getByOrderId(String orderId) {
        return loadPaymentPort.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("order:" + orderId));
    }
}

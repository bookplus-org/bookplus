package com.bookplus.payment.application.payment;

import com.bookplus.payment.domain.exception.UnsupportedPaymentMethodException;
import com.bookplus.payment.domain.model.PaymentMethod;
import com.bookplus.payment.domain.service.PaymentMethodHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selecciona el {@link PaymentMethodHandler} adecuado para un método de pago.
 * Spring inyecta todos los handlers registrados; añadir uno nuevo no requiere
 * tocar esta clase (Open/Closed).
 */
@Component
public class PaymentMethodResolver {

    private final List<PaymentMethodHandler> handlers;

    public PaymentMethodResolver(List<PaymentMethodHandler> handlers) {
        this.handlers = handlers;
    }

    public PaymentMethodHandler resolve(PaymentMethod method) {
        return handlers.stream()
                .filter(h -> h.supports(method))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException(method));
    }
}

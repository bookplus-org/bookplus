package com.bookplus.payment.domain.event;

import com.bookplus.payment.domain.model.*;
import lombok.Getter;

import java.time.Instant;

/** Consumed by order-service → transitions order to PAYMENT_PROCESSING */
@Getter
public class PaymentInitiatedEvent implements DomainEvent {

    private final String        paymentId;
    private final String        orderId;
    private final String        userId;
    private final Money         amount;
    private final PaymentMethod paymentMethod;
    private final Instant       occurredOn;

    public PaymentInitiatedEvent(String paymentId, String orderId, String userId,
                                 Money amount, PaymentMethod paymentMethod) {
        this.paymentId     = paymentId;
        this.orderId       = orderId;
        this.userId        = userId;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.occurredOn    = Instant.now();
    }

    @Override public String  aggregateId() { return paymentId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

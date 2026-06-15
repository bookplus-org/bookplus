package com.bookplus.payment.domain.event;

import com.bookplus.payment.domain.model.*;
import lombok.Getter;

import java.time.Instant;

/** Consumed by order-service → transitions order to CONFIRMED; notification-service → success email */
@Getter
public class PaymentCompletedEvent implements DomainEvent {

    private final String  paymentId;
    private final String  orderId;
    private final String  userId;
    private final Money   amount;
    private final String  transactionRef;  // external gateway reference
    private final Instant occurredOn;

    public PaymentCompletedEvent(String paymentId, String orderId, String userId,
                                 Money amount, String transactionRef) {
        this.paymentId      = paymentId;
        this.orderId        = orderId;
        this.userId         = userId;
        this.amount         = amount;
        this.transactionRef = transactionRef;
        this.occurredOn     = Instant.now();
    }

    @Override public String  aggregateId() { return paymentId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

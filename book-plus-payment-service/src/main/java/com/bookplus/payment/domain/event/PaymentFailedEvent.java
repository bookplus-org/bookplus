package com.bookplus.payment.domain.event;

import com.bookplus.payment.domain.model.*;
import lombok.Getter;

import java.time.Instant;

/** Consumed by order-service → cancels order; notification-service → failure email */
@Getter
public class PaymentFailedEvent implements DomainEvent {

    private final String  paymentId;
    private final String  orderId;
    private final String  userId;
    private final String  reason;
    private final Instant occurredOn;

    public PaymentFailedEvent(String paymentId, String orderId, String userId, String reason) {
        this.paymentId  = paymentId;
        this.orderId    = orderId;
        this.userId     = userId;
        this.reason     = reason;
        this.occurredOn = Instant.now();
    }

    @Override public String  aggregateId() { return paymentId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

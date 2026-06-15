package com.bookplus.payment.domain.event;

import com.bookplus.payment.domain.model.*;
import lombok.Getter;

import java.time.Instant;

/** Emitted when a refund is initiated (usually after order cancellation post-payment) */
@Getter
public class RefundInitiatedEvent implements DomainEvent {

    private final String  paymentId;
    private final String  orderId;
    private final String  userId;
    private final Money   refundAmount;
    private final String  reason;
    private final Instant occurredOn;

    public RefundInitiatedEvent(String paymentId, String orderId, String userId,
                                Money refundAmount, String reason) {
        this.paymentId    = paymentId;
        this.orderId      = orderId;
        this.userId       = userId;
        this.refundAmount = refundAmount;
        this.reason       = reason;
        this.occurredOn   = Instant.now();
    }

    @Override public String  aggregateId() { return paymentId; }
    @Override public Instant occurredOn()  { return occurredOn; }
}

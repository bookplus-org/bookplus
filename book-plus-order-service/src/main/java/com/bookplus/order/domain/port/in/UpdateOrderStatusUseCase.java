package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.*;

public interface UpdateOrderStatusUseCase {

    /** Called by payment-service Kafka event: payment initiated */
    Order startPaymentProcessing(String orderId, String paymentId);

    /** Called by payment-service Kafka event: payment confirmed */
    Order confirmPayment(String orderId);

    /** Called by admin/warehouse operation — registers carrier + tracking number. */
    Order ship(String orderId, String carrier, String trackingNumber);

    /** Admin/warehouse marks delivered — must provide the customer's delivery code. */
    Order deliver(String orderId, String deliveryCode, String receivedBy);

    /** The customer confirms they received the order (their own proof). */
    Order confirmReceipt(String orderId, String requestingUserId);

    /** A courier claims an unassigned delivery (self-assignment). */
    Order claimDelivery(String orderId, String courierUserId, String courierName);

    /** The customer opens a claim/dispute on their order. */
    Order openClaim(String orderId, String requestingUserId, String reason);

    /** Admin resolves an open claim with a note. */
    Order resolveClaim(String orderId, String resolution);

    /** Admin issues a refund (devolución) on a paid order; restock returns items to inventory. */
    Order refund(String orderId, String reason, boolean restock);
}

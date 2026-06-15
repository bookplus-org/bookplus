package com.bookplus.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Raw order event record — stored as-is for audit and ad-hoc reporting.
 */
@Getter
@Builder
public class OrderEvent {
    private final String        orderId;
    private final String        userId;
    private final String        eventType;   // ORDER_CREATED, ORDER_CANCELLED, etc.
    private final BigDecimal    total;
    private final String        currency;
    private final List<ItemSnapshot> items;
    private final Instant       occurredOn;

    @Getter
    @Builder
    public static class ItemSnapshot {
        private final String     bookId;
        private final String     isbn;
        private final String     title;
        private final int        quantity;
        private final BigDecimal unitPrice;
    }
}

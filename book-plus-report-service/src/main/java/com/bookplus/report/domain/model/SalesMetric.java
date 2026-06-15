package com.bookplus.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily pre-aggregated sales snapshot consumed from order + payment events.
 * Stored in report_db for fast dashboard queries.
 */
@Getter
@Builder
public class SalesMetric {
    private final LocalDate   date;
    private final int         ordersCount;
    private final int         itemsSold;
    private final BigDecimal  revenue;
    private final String      currency;
    private final int         cancellations;
    private final int         refunds;
    private final BigDecimal  refundedAmount;
}

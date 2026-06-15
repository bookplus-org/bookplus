package com.bookplus.report.domain.port.in;

import com.bookplus.report.domain.model.*;

import java.time.LocalDate;
import java.util.List;

public interface GetSalesDashboardUseCase {

    /** Daily metrics between two dates (inclusive) */
    List<SalesMetric> getDailyMetrics(LocalDate from, LocalDate to);

    /** Aggregated summary for a period */
    SalesSummary getSummary(LocalDate from, LocalDate to);

    /** Top N books by units sold in the period */
    List<TopBook> getTopBooks(LocalDate from, LocalDate to, int limit);

    record SalesSummary(
            int         totalOrders,
            int         totalItemsSold,
            java.math.BigDecimal totalRevenue,
            String      currency,
            int         totalCancellations,
            java.math.BigDecimal totalRefunded
    ) {}
}

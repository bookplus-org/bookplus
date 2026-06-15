package com.bookplus.report.application.usecase;

import com.bookplus.report.domain.model.*;
import com.bookplus.report.domain.port.in.GetSalesDashboardUseCase;
import com.bookplus.report.domain.port.out.LoadMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSalesDashboardUseCaseImpl implements GetSalesDashboardUseCase {

    private final LoadMetricsPort loadMetricsPort;

    @Override
    public List<SalesMetric> getDailyMetrics(LocalDate from, LocalDate to) {
        return loadMetricsPort.findDailyMetrics(from, to);
    }

    @Override
    public SalesSummary getSummary(LocalDate from, LocalDate to) {
        List<SalesMetric> metrics = loadMetricsPort.findDailyMetrics(from, to);

        int         totalOrders        = metrics.stream().mapToInt(SalesMetric::getOrdersCount).sum();
        int         totalItems         = metrics.stream().mapToInt(SalesMetric::getItemsSold).sum();
        BigDecimal  totalRevenue       = metrics.stream().map(SalesMetric::getRevenue)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int         totalCancellations = metrics.stream().mapToInt(SalesMetric::getCancellations).sum();
        BigDecimal  totalRefunded      = metrics.stream().map(SalesMetric::getRefundedAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = metrics.isEmpty() ? "USD" : metrics.get(0).getCurrency();

        return new SalesSummary(totalOrders, totalItems, totalRevenue,
                currency, totalCancellations, totalRefunded);
    }

    @Override
    public List<TopBook> getTopBooks(LocalDate from, LocalDate to, int limit) {
        return loadMetricsPort.findTopBooks(from, to, limit);
    }
}

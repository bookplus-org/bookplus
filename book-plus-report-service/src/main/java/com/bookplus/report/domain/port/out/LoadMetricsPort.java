package com.bookplus.report.domain.port.out;

import com.bookplus.report.domain.model.*;

import java.time.LocalDate;
import java.util.List;

public interface LoadMetricsPort {
    List<SalesMetric> findDailyMetrics(LocalDate from, LocalDate to);
    List<TopBook>     findTopBooks(LocalDate from, LocalDate to, int limit);
}

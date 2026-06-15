package com.bookplus.report.domain.port.in;

import java.time.LocalDate;

public interface ExportReportUseCase {
    /** Returns CSV bytes of daily sales metrics */
    byte[] exportSalesCsv(LocalDate from, LocalDate to);

    /** Returns PDF bytes of sales summary report */
    byte[] exportSalesPdf(LocalDate from, LocalDate to);
}

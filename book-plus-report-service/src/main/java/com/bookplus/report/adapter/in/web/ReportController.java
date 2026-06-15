package com.bookplus.report.adapter.in.web;

import com.bookplus.report.domain.model.*;
import com.bookplus.report.domain.port.in.*;
import com.bookplus.report.domain.port.in.GetSalesDashboardUseCase.SalesSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Sales metrics, dashboards and exports — ADMIN only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class ReportController {

    private final GetSalesDashboardUseCase dashboardUseCase;
    private final ExportReportUseCase      exportUseCase;

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/sales/summary")
    @Operation(summary = "Aggregated sales summary for a date range")
    public ResponseEntity<SalesSummary> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardUseCase.getSummary(from, to));
    }

    @GetMapping("/sales/daily")
    @Operation(summary = "Day-by-day sales metrics for a date range")
    public ResponseEntity<List<SalesMetric>> getDailyMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardUseCase.getDailyMetrics(from, to));
    }

    @GetMapping("/sales/top-books")
    @Operation(summary = "Top N best-selling books for a date range")
    public ResponseEntity<List<TopBook>> getTopBooks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(dashboardUseCase.getTopBooks(from, to, limit));
    }

    // ── Exports ───────────────────────────────────────────────────────────

    @GetMapping("/sales/export/csv")
    @Operation(summary = "Download daily sales metrics as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] csv = exportUseCase.exportSalesCsv(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"sales_" + from + "_" + to + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/sales/export/pdf")
    @Operation(summary = "Download sales summary report as PDF")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] pdf = exportUseCase.exportSalesPdf(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"sales_" + from + "_" + to + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

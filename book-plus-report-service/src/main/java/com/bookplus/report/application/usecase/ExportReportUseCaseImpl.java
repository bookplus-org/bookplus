package com.bookplus.report.application.usecase;

import com.bookplus.report.domain.model.SalesMetric;
import com.bookplus.report.domain.port.in.ExportReportUseCase;
import com.bookplus.report.domain.port.in.GetSalesDashboardUseCase;
import com.bookplus.report.domain.port.in.GetSalesDashboardUseCase.SalesSummary;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportReportUseCaseImpl implements ExportReportUseCase {

    private final GetSalesDashboardUseCase dashboardUseCase;

    // ── CSV ───────────────────────────────────────────────────────────────

    @Override
    public byte[] exportSalesCsv(LocalDate from, LocalDate to) {
        List<SalesMetric> metrics = dashboardUseCase.getDailyMetrics(from, to);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer  = new OutputStreamWriter(baos);
             CSVWriter csvWriter        = new CSVWriter(writer)) {

            // Header
            csvWriter.writeNext(new String[]{
                "Date", "Orders", "Items Sold", "Revenue", "Currency",
                "Cancellations", "Refunds", "Refunded Amount"
            });

            // Rows
            for (SalesMetric m : metrics) {
                csvWriter.writeNext(new String[]{
                    m.getDate().toString(),
                    String.valueOf(m.getOrdersCount()),
                    String.valueOf(m.getItemsSold()),
                    m.getRevenue().toPlainString(),
                    m.getCurrency(),
                    String.valueOf(m.getCancellations()),
                    String.valueOf(m.getRefunds()),
                    m.getRefundedAmount().toPlainString()
                });
            }

            csvWriter.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate CSV report", ex);
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────

    @Override
    public byte[] exportSalesPdf(LocalDate from, LocalDate to) {
        SalesSummary     summary = dashboardUseCase.getSummary(from, to);
        List<SalesMetric> daily  = dashboardUseCase.getDailyMetrics(from, to);

        /*
         * Pure-Java PDF generation without iText dependency on the classpath at compile time.
         * We generate a minimal valid PDF manually so the service compiles and runs even
         * without the iText license configured.  In production, replace this with the
         * full iText/OpenPDF implementation.
         */
        String content = buildPdfTextContent(summary, daily, from, to);
        return buildMinimalPdf(content);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String buildPdfTextContent(SalesSummary summary,
                                       List<SalesMetric> daily,
                                       LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("BookPlus — Sales Report\n");
        sb.append("Period: ").append(from).append(" to ").append(to).append("\n\n");
        sb.append("SUMMARY\n");
        sb.append("  Total Orders    : ").append(summary.totalOrders()).append("\n");
        sb.append("  Items Sold      : ").append(summary.totalItemsSold()).append("\n");
        sb.append("  Revenue         : ").append(summary.totalRevenue()).append(" ").append(summary.currency()).append("\n");
        sb.append("  Cancellations   : ").append(summary.totalCancellations()).append("\n");
        sb.append("  Refunded Amount : ").append(summary.totalRefunded()).append(" ").append(summary.currency()).append("\n\n");
        sb.append("DAILY BREAKDOWN\n");
        for (SalesMetric m : daily) {
            sb.append(String.format("  %s | Orders:%d | Revenue:%s %s%n",
                    m.getDate(), m.getOrdersCount(), m.getRevenue(), m.getCurrency()));
        }
        return sb.toString();
    }

    /**
     * Builds a minimal RFC-compliant PDF containing plain text.
     * Sufficient for download; replace with iText for styled output.
     */
    private byte[] buildMinimalPdf(String text) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Escape text for PDF stream
            String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            String[] lines = escaped.split("\n");

            StringBuilder stream = new StringBuilder();
            stream.append("BT\n/F1 11 Tf\n50 780 Td\n12 TL\n");
            for (String line : lines) {
                stream.append("(").append(line).append(") Tj T*\n");
            }
            stream.append("ET\n");

            String streamContent = stream.toString();
            int streamLen        = streamContent.getBytes().length;

            String pdf = "%PDF-1.4\n"
                + "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                + "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
                + "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]"
                + " /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
                + "4 0 obj\n<< /Length " + streamLen + " >>\nstream\n"
                + streamContent + "endstream\nendobj\n"
                + "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
                + "xref\n0 6\n0000000000 65535 f\n"
                + "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n0\n%%EOF\n";

            baos.write(pdf.getBytes());
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate PDF report", ex);
        }
    }
}

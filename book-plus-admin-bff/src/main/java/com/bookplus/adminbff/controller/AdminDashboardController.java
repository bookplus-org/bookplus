package com.bookplus.adminbff.controller;

import com.bookplus.adminbff.service.DownstreamClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;

/**
 * Aggregated dashboard — combines data from multiple services into a single response.
 * The admin panel calls ONE endpoint instead of N services.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Aggregated admin panel data — ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DownstreamClient client;

    /**
     * Full dashboard snapshot — merges sales summary + inventory summary.
     * Called once on admin panel load.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Aggregated dashboard: sales summary + inventory overview")
    public ResponseEntity<Map<String, Object>> dashboard(
            HttpServletRequest request,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(30)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String token = request.getHeader("Authorization");

        Map<String, Object> salesSummary  = client.reports(
                "/api/v1/reports/sales/summary?from=" + from + "&to=" + to, token);
        Map<String, Object> topBooks      = Map.of("topBooks",
                client.getList(client.getClass().getSimpleName(),
                        "/api/v1/reports/sales/top-books?from=" + from + "&to=" + to + "&limit=5", token));

        return ResponseEntity.ok(Map.of(
                "period",      Map.of("from", from.toString(), "to", to.toString()),
                "salesSummary", salesSummary,
                "topBooks",    topBooks
        ));
    }

    // ── Orders proxy ──────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(summary = "List all orders (proxied from order-service)")
    public ResponseEntity<Map<String, Object>> listOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String token = request.getHeader("Authorization");
        return ResponseEntity.ok(client.orders(
                "/api/v1/orders?page=" + page + "&size=" + size, token));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details (proxied from order-service)")
    public ResponseEntity<Map<String, Object>> getOrder(
            @PathVariable String orderId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(client.orders("/api/v1/orders/" + orderId,
                request.getHeader("Authorization")));
    }

    // ── Inventory proxy ───────────────────────────────────────────────────

    @GetMapping("/inventory/{bookId}")
    @Operation(summary = "Get stock for a book (proxied from inventory-service)")
    public ResponseEntity<Map<String, Object>> getStock(
            @PathVariable String bookId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(client.inventory("/api/v1/inventory/" + bookId,
                request.getHeader("Authorization")));
    }

    // ── Catalog proxy ─────────────────────────────────────────────────────

    @GetMapping("/books")
    @Operation(summary = "List books (proxied from catalog-service)")
    public ResponseEntity<Map<String, Object>> listBooks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(client.catalog(
                "/api/v1/books?page=" + page + "&size=" + size,
                request.getHeader("Authorization")));
    }

    // ── Reports proxy ─────────────────────────────────────────────────────

    @GetMapping("/reports/sales/summary")
    @Operation(summary = "Sales summary (proxied from report-service)")
    public ResponseEntity<Map<String, Object>> salesSummary(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(client.reports(
                "/api/v1/reports/sales/summary?from=" + from + "&to=" + to,
                request.getHeader("Authorization")));
    }

    @GetMapping("/reports/sales/daily")
    @Operation(summary = "Daily sales metrics (proxied from report-service)")
    public ResponseEntity<Map<String, Object>> dailySales(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(client.reports(
                "/api/v1/reports/sales/daily?from=" + from + "&to=" + to,
                request.getHeader("Authorization")));
    }
}

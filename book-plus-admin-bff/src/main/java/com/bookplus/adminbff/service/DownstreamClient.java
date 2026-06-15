package com.bookplus.adminbff.service;

import com.bookplus.adminbff.config.ServicesProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Resilient downstream HTTP client for the Admin BFF.
 *
 * Each service has its own named circuit breaker (see application.yml).
 * When a circuit is OPEN, calls return an immediate fallback response
 * instead of waiting for the timeout — keeping the admin panel usable
 * even when one downstream service is degraded.
 *
 * Circuit breaker names match the resilience4j config keys:
 *   catalog, inventory, orders, payments, reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DownstreamClient {

    private final WebClient.Builder  webClientBuilder;
    private final ServicesProperties services;

    // ── Catalog ───────────────────────────────────────────────────────────

    @CircuitBreaker(name = "catalog", fallbackMethod = "fallbackMap")
    @Retry(name = "catalog")
    public Map<String, Object> catalog(String path, String token) {
        return get(services.getCatalogUrl(), path, token);
    }

    // ── Inventory ─────────────────────────────────────────────────────────

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMap")
    @Retry(name = "inventory")
    public Map<String, Object> inventory(String path, String token) {
        return get(services.getInventoryUrl(), path, token);
    }

    // ── Orders ────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "orders", fallbackMethod = "fallbackMap")
    @Retry(name = "orders")
    public Map<String, Object> orders(String path, String token) {
        return get(services.getOrderUrl(), path, token);
    }

    // ── Payments ──────────────────────────────────────────────────────────

    @CircuitBreaker(name = "payments", fallbackMethod = "fallbackMap")
    @Retry(name = "payments")
    public Map<String, Object> payments(String path, String token) {
        return get(services.getPaymentUrl(), path, token);
    }

    // ── Reports ───────────────────────────────────────────────────────────

    @CircuitBreaker(name = "reports", fallbackMethod = "fallbackMap")
    @Retry(name = "reports")
    public Map<String, Object> reports(String path, String token) {
        return get(services.getReportUrl(), path, token);
    }

    // ── Shared GET helpers ────────────────────────────────────────────────

    public Map<String, Object> get(String baseUrl, String path, String token) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = webClientBuilder.build()
                .get()
                .uri(baseUrl + path)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        return result != null ? result : Map.of();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getList(String baseUrl, String path, String token) {
        List<Map<String, Object>> result = webClientBuilder.build()
                .get()
                .uri(baseUrl + path)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(List.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        return result != null ? result : List.of();
    }

    // ── Fallbacks (degraded mode) ─────────────────────────────────────────

    /**
     * Called when the circuit is OPEN or all retries are exhausted.
     * Returns a structured degraded response so the admin panel can
     * show a partial dashboard instead of a full error page.
     */
    public Map<String, Object> fallbackMap(String path, String token, Throwable ex) {
        String service = extractServiceName(path);
        log.warn("Circuit breaker fallback for service={} path={}: {}", service, path, ex.getMessage());
        return Map.of(
                "degraded",      true,
                "service",       service,
                "message",       "Service temporarily unavailable — showing cached or partial data",
                "errorType",     ex.getClass().getSimpleName()
        );
    }

    private String extractServiceName(String path) {
        if (path == null) return "unknown";
        if (path.contains("catalog"))      return "catalog-service";
        if (path.contains("inventory"))    return "inventory-service";
        if (path.contains("order"))        return "order-service";
        if (path.contains("payment"))      return "payment-service";
        if (path.contains("report"))       return "report-service";
        return "unknown-service";
    }
}

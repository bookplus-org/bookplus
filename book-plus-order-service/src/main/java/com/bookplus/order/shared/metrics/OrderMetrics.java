package com.bookplus.order.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de negocio del pedido (Micrometer → Prometheus → Grafana).
 *
 * A diferencia de las métricas técnicas (CPU, latencia), estas miden el negocio: cuántos
 * pedidos se crean y por qué método de pago, y el importe acumulado. Permiten paneles y
 * alertas de negocio (p. ej. "han caído los pedidos con tarjeta en la última hora").
 */
@Component
public class OrderMetrics {

    private final MeterRegistry registry;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Registra un pedido creado: incrementa el contador (por método de pago) y acumula importe. */
    public void recordOrderCreated(String paymentMethod, String currency, double amount) {
        Counter.builder("bookplus.orders.created")
                .description("Número de pedidos creados")
                .tag("payment_method", paymentMethod == null ? "unknown" : paymentMethod)
                .register(registry)
                .increment();

        DistributionSummary.builder("bookplus.orders.amount")
                .description("Importe de los pedidos creados")
                .baseUnit("currency")
                .tag("currency", currency == null ? "unknown" : currency)
                .register(registry)
                .record(amount);
    }
}

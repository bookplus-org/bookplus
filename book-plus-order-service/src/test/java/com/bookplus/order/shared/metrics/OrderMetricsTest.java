package com.bookplus.order.shared.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las métricas de negocio del pedido con un registro en memoria (sin Spring).
 */
class OrderMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OrderMetrics metrics = new OrderMetrics(registry);

    @Test
    void cuenta_pedidos_por_metodo_de_pago_y_acumula_importe() {
        metrics.recordOrderCreated("CARD", "USD", 42.0);
        metrics.recordOrderCreated("CARD", "USD", 8.0);
        metrics.recordOrderCreated("PAYPAL", "USD", 100.0);

        assertThat(registry.get("bookplus.orders.created")
                .tag("payment_method", "CARD").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("bookplus.orders.created")
                .tag("payment_method", "PAYPAL").counter().count()).isEqualTo(1.0);

        DistributionSummary amount = registry.get("bookplus.orders.amount").summary();
        assertThat(amount.count()).isEqualTo(3);
        assertThat(amount.totalAmount()).isEqualTo(150.0);
    }
}

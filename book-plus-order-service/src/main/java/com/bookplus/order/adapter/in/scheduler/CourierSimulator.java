package com.bookplus.order.adapter.in.scheduler;

import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.model.OrderStatus;
import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import com.bookplus.order.domain.port.out.LoadOrderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Courier simulado: imita el webhook de confirmación de entrega de una PAQUETERÍA
 * EXTERNA, pasando esos pedidos ENVIADO → ENTREGADO automáticamente tras un tiempo.
 *
 * No toca los envíos de "reparto propio": esos los entrega una persona y deben
 * confirmarse con la prueba real (foto + firma + código), o el propio cliente con
 * "Confirmar recepción". Así la entrega automática nunca salta la prueba de entrega.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourierSimulator {

    private static final Duration DELIVERY_DELAY = Duration.ofMinutes(3);
    /** Debe coincidir con el valor del diálogo de envío del frontend (ship-dialog). */
    private static final String OWN_DELIVERY = "Reparto propio (personal)";

    private final LoadOrderPort            loadOrderPort;
    private final UpdateOrderStatusUseCase updateStatusUseCase;

    @Scheduled(fixedDelay = 60_000) // cada minuto
    public void autoDeliverShippedOrders() {
        Instant cutoff = Instant.now().minus(DELIVERY_DELAY);
        for (Order order : loadOrderPort.findByStatus(OrderStatus.SHIPPED)) {
            String carrier = order.getCarrier();
            boolean ownDelivery = carrier == null || carrier.isBlank() || OWN_DELIVERY.equals(carrier);
            if (ownDelivery) {
                // Entrega personal: requiere prueba real, no se auto-entrega.
                continue;
            }
            if (order.getUpdatedAt() != null && order.getUpdatedAt().isBefore(cutoff)) {
                try {
                    // La agencia confirma la entrega usando el código del pedido (prueba).
                    updateStatusUseCase.deliver(order.getId().toString(),
                            order.getDeliveryCode(), "Confirmado por " + carrier);
                    log.info("Courier sim: order {} auto-delivered via {}", order.getId(), carrier);
                } catch (Exception ex) {
                    log.warn("Courier sim could not deliver {}: {}", order.getId(), ex.getMessage());
                }
            }
        }
    }
}

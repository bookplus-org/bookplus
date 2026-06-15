package com.bookplus.inventory.adapter.in.messaging;

import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockReservation;
import com.bookplus.inventory.domain.port.in.AddStockUseCase;
import com.bookplus.inventory.domain.port.in.AddStockUseCase.AddStockCommand;
import com.bookplus.inventory.domain.port.in.ConfirmReservationUseCase;
import com.bookplus.inventory.domain.port.in.ConfirmReservationUseCase.ConfirmReservationCommand;
import com.bookplus.inventory.domain.port.in.InitializeStockUseCase;
import com.bookplus.inventory.domain.port.in.InitializeStockUseCase.InitializeStockCommand;
import com.bookplus.inventory.domain.port.in.ReleaseReservationUseCase;
import com.bookplus.inventory.domain.port.in.ReleaseReservationUseCase.ReleaseReservationCommand;
import com.bookplus.inventory.domain.port.in.ReserveStockUseCase;
import com.bookplus.inventory.domain.port.in.ReserveStockUseCase.ReserveStockCommand;
import com.bookplus.inventory.domain.port.out.LoadReservationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter IN — Kafka consumer del flujo de pedidos (saga de stock).
 *
 *   order.created            → reservar stock por cada ítem
 *   order.payment.confirmed  → confirmar reserva (descuento definitivo)
 *   order.cancelled          → liberar reserva
 *
 * Cada ítem se procesa de forma best-effort e idempotente: un fallo aislado
 * (p. ej. sin stock o reserva inexistente) se registra sin abortar el resto.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final int DEFAULT_STOCK     = 1000;
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ReserveStockUseCase       reserveStockUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final InitializeStockUseCase    initializeStockUseCase;
    private final LoadReservationPort       loadReservationPort;
    private final AddStockUseCase           addStockUseCase;

    // ── order.created → reservar ───────────────────────────────────────────
    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> payload = record.value();
        String orderId = asString(payload.get("orderId"));
        String userId  = asString(payload.get("userId"));
        log.info("Received order.created: orderId={} — reserving stock", orderId);

        for (Map<String, Object> item : items(payload)) {
            String bookId = asString(item.get("bookId"));
            int    qty    = intOf(item.get("quantity"));
            try {
                reserveStockUseCase.reserve(new ReserveStockCommand(bookId, orderId, userId, qty));
            } catch (StockNotFoundException notFound) {
                // Demo convenience: catalog books may not have an inventory record yet.
                // Initialize with a default stock and retry once so the saga proceeds.
                log.info("No stock record for bookId={}; initializing with {} units", bookId, DEFAULT_STOCK);
                try {
                    initializeStockUseCase.initialize(
                            new InitializeStockCommand(bookId, DEFAULT_STOCK, LOW_STOCK_THRESHOLD));
                    reserveStockUseCase.reserve(new ReserveStockCommand(bookId, orderId, userId, qty));
                } catch (Exception retryEx) {
                    log.error("Could not reserve stock after init: orderId={} bookId={} — {}",
                            orderId, bookId, retryEx.getMessage());
                }
            } catch (Exception ex) {
                log.error("Could not reserve stock: orderId={} bookId={} qty={} — {}",
                        orderId, bookId, qty, ex.getMessage());
            }
        }
    }

    // ── order.payment.confirmed → confirmar reserva ────────────────────────
    @KafkaListener(
            topics = "order.payment.confirmed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> payload = record.value();
        String orderId = asString(payload.get("orderId"));
        log.info("Received order.payment.confirmed: orderId={} — confirming reservations", orderId);

        for (Map<String, Object> item : items(payload)) {
            String bookId = asString(item.get("bookId"));
            Optional<StockReservation> reservation =
                    loadReservationPort.findByOrderIdAndBookId(orderId, BookId.of(bookId));
            if (reservation.isEmpty()) {
                log.warn("No reservation to confirm: orderId={} bookId={}", orderId, bookId);
                continue;
            }
            try {
                confirmReservationUseCase.confirm(
                        new ConfirmReservationCommand(reservation.get().getId().toString(), orderId));
            } catch (Exception ex) {
                log.error("Could not confirm reservation: orderId={} bookId={} — {}",
                        orderId, bookId, ex.getMessage());
            }
        }
    }

    // ── order.cancelled → liberar reserva ──────────────────────────────────
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCancelled(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> payload = record.value();
        String orderId = asString(payload.get("orderId"));
        String reason  = asString(payload.getOrDefault("reason", "Order cancelled"));
        log.info("Received order.cancelled: orderId={} — releasing reservations", orderId);

        for (Map<String, Object> item : items(payload)) {
            String bookId = asString(item.get("bookId"));
            Optional<StockReservation> reservation =
                    loadReservationPort.findByOrderIdAndBookId(orderId, BookId.of(bookId));
            if (reservation.isEmpty()) {
                log.warn("No reservation to release: orderId={} bookId={}", orderId, bookId);
                continue;
            }
            try {
                releaseReservationUseCase.release(new ReleaseReservationCommand(
                        reservation.get().getId().toString(), orderId, reason));
            } catch (Exception ex) {
                log.error("Could not release reservation: orderId={} bookId={} — {}",
                        orderId, bookId, ex.getMessage());
            }
        }
    }

    // ── order.refunded → reponer stock (devolución revendible) ─────────────
    @KafkaListener(
            topics = "order.refunded",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderRefunded(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> payload = record.value();
        String orderId = asString(payload.get("orderId"));
        log.info("Received order.refunded: orderId={} — restocking items", orderId);

        for (Map<String, Object> item : items(payload)) {
            String bookId   = asString(item.get("bookId"));
            int    quantity = intOf(item.get("quantity"));
            try {
                addStockUseCase.addStock(new AddStockCommand(
                        bookId, quantity, orderId, "Reposición por devolución/reembolso"));
            } catch (Exception ex) {
                log.error("Could not restock on refund: orderId={} bookId={} — {}",
                        orderId, bookId, ex.getMessage());
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> payload) {
        Object raw = payload.get("items");
        return raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
    }

    /** Value objects pueden serializarse como {"value":"..."}; los aplanamos. */
    private static String asString(Object o) {
        if (o instanceof Map<?, ?> m && m.containsKey("value")) {
            return String.valueOf(m.get("value"));
        }
        return o == null ? null : o.toString();
    }

    private static int intOf(Object o) {
        return o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o));
    }
}

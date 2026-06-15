package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.event.StockReleasedEvent;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Job periódico — libera reservas vencidas (TTL expirado).
 * Se ejecuta cada 5 minutos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireReservationsScheduler {

    private final LoadReservationPort      loadReservationPort;
    private final SaveReservationPort      saveReservationPort;
    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Scheduled(fixedDelay = 5 * 60 * 1000)  // cada 5 minutos
    public void expireStaleReservations() {
        List<StockReservation> expired = loadReservationPort.findExpiredPending(Instant.now());

        if (expired.isEmpty()) return;

        log.info("Expiring {} stale reservations", expired.size());

        for (StockReservation reservation : expired) {
            try {
                reservation.expire();

                loadStockPort.findByBookId(reservation.getBookId()).ifPresent(stock -> {
                    StockMovement movement = stock.releaseReservation(
                            reservation.getQuantity(),
                            reservation.getOrderId(),
                            "Reservation expired (TTL)"
                    );
                    saveStockPort.save(stock);
                    saveMovementPort.save(movement);

                    eventPublisher.publishAll(stock.pullDomainEvents());
                    eventPublisher.publish(new StockReleasedEvent(
                            reservation.getId(), reservation.getBookId(),
                            reservation.getOrderId(), reservation.getQuantity(),
                            "Reservation expired"));
                });

                saveReservationPort.save(reservation);

                log.debug("Expired reservation: id={} orderId={}",
                        reservation.getId(), reservation.getOrderId());

            } catch (Exception ex) {
                log.error("Failed to expire reservation {}: {}", reservation.getId(), ex.getMessage());
            }
        }
    }
}

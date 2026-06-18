package com.bookplus.inventory.application.batch;

import com.bookplus.inventory.domain.event.StockReleasedEvent;
import com.bookplus.inventory.domain.model.StockMovement;
import com.bookplus.inventory.domain.model.StockReservation;
import com.bookplus.inventory.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lógica de expiración de una reserva vencida: la marca como EXPIRED, libera el stock
 * reservado y publica los eventos. Es el "writer" del job de Spring Batch y, al estar
 * aislada de la infraestructura batch, se puede probar de forma unitaria con mocks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryService {

    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final SaveReservationPort      saveReservationPort;
    private final DomainEventPublisherPort eventPublisher;

    /** Expira una reserva PENDING vencida y devuelve sus unidades al stock disponible. */
    public void expire(StockReservation reservation) {
        reservation.expire();

        loadStockPort.findByBookId(reservation.getBookId()).ifPresent(stock -> {
            StockMovement movement = stock.releaseReservation(
                    reservation.getQuantity(), reservation.getOrderId(), "Reservation expired (TTL)");
            saveStockPort.save(stock);
            saveMovementPort.save(movement);
            eventPublisher.publishAll(stock.pullDomainEvents());
            eventPublisher.publish(new StockReleasedEvent(
                    reservation.getId(), reservation.getBookId(),
                    reservation.getOrderId(), reservation.getQuantity(), "Reservation expired"));
        });

        saveReservationPort.save(reservation);
        log.debug("Expired reservation: id={} orderId={}", reservation.getId(), reservation.getOrderId());
    }
}

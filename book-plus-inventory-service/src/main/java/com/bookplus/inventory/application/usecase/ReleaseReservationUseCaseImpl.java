package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.event.StockReleasedEvent;
import com.bookplus.inventory.domain.exception.ReservationNotFoundException;
import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.ReleaseReservationUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class ReleaseReservationUseCaseImpl implements ReleaseReservationUseCase {

    private final LoadReservationPort      loadReservationPort;
    private final SaveReservationPort      saveReservationPort;
    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public void release(ReleaseReservationCommand command) {
        log.debug("Releasing reservation: id={} orderId={}", command.reservationId(), command.orderId());

        StockReservation reservation = loadReservationPort
                .findById(ReservationId.of(command.reservationId()))
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        reservation.cancel();

        Stock stock = loadStockPort.findByBookId(reservation.getBookId())
                .orElseThrow(() -> new StockNotFoundException(reservation.getBookId().toString()));

        StockMovement movement = stock.releaseReservation(
                reservation.getQuantity(), command.orderId(), command.reason());

        saveReservationPort.save(reservation);
        saveStockPort.save(stock);
        saveMovementPort.save(movement);

        try {
            eventPublisher.publishAll(stock.pullDomainEvents());
            eventPublisher.publish(new StockReleasedEvent(
                    reservation.getId(), reservation.getBookId(),
                    command.orderId(), reservation.getQuantity(), command.reason()));
        } catch (Exception ex) {
            log.error("Failed to publish events on release reservation {}: {}", command.reservationId(), ex.getMessage());
        }

        log.info("Reservation released: id={} orderId={} qty={}",
                command.reservationId(), command.orderId(), reservation.getQuantity());
    }
}

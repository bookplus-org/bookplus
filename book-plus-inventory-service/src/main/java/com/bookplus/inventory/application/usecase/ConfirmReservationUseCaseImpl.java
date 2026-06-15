package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.ReservationNotFoundException;
import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.ConfirmReservationUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class ConfirmReservationUseCaseImpl implements ConfirmReservationUseCase {

    private final LoadReservationPort      loadReservationPort;
    private final SaveReservationPort      saveReservationPort;
    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public void confirm(ConfirmReservationCommand command) {
        log.debug("Confirming reservation: id={} orderId={}", command.reservationId(), command.orderId());

        // 1. Cargar la reserva
        StockReservation reservation = loadReservationPort
                .findById(ReservationId.of(command.reservationId()))
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        // 2. Confirmar la reserva en el dominio
        reservation.confirm();

        // 3. Confirmar en el stock (descuento definitivo)
        Stock stock = loadStockPort.findByBookId(reservation.getBookId())
                .orElseThrow(() -> new StockNotFoundException(reservation.getBookId().toString()));

        StockMovement movement = stock.confirmReservation(
                reservation.getQuantity(), command.orderId());

        // 4. Persistir
        saveReservationPort.save(reservation);
        saveStockPort.save(stock);
        saveMovementPort.save(movement);

        // 5. Publicar eventos
        try {
            eventPublisher.publishAll(stock.pullDomainEvents());
        } catch (Exception ex) {
            log.error("Failed to publish events on confirm reservation {}: {}", command.reservationId(), ex.getMessage());
        }

        log.info("Reservation confirmed: id={} orderId={} qty={}",
                command.reservationId(), command.orderId(), reservation.getQuantity());
    }
}

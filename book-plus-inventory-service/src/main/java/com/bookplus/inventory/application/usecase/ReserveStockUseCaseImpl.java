package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.event.StockReservedEvent;
import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.ReserveStockUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class ReserveStockUseCaseImpl implements ReserveStockUseCase {

    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveReservationPort      saveReservationPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public StockReservation reserve(ReserveStockCommand command) {
        log.debug("Reserving stock: bookId={} orderId={} qty={}",
                command.bookId(), command.orderId(), command.quantity());

        BookId bookId = BookId.of(command.bookId());

        // 1. Cargar stock y reservar (lanza InsufficientStockException si no hay)
        Stock stock = loadStockPort.findByBookId(bookId)
                .orElseThrow(() -> new StockNotFoundException(command.bookId()));

        StockMovement movement = stock.reserve(command.quantity(), command.orderId());

        // 2. Crear la entidad de reserva
        StockReservation reservation = StockReservation.create(
                bookId, command.orderId(), command.userId(), command.quantity());

        // 3. Persistir: stock actualizado, reserva y movimiento
        saveStockPort.save(stock);
        StockReservation saved = saveReservationPort.save(reservation);
        saveMovementPort.save(movement);

        // 4. Publicar eventos (StockUpdated + LowStockAlert si aplica + StockReserved)
        try {
            eventPublisher.publishAll(stock.pullDomainEvents());
            eventPublisher.publish(new StockReservedEvent(
                    saved.getId(), bookId,
                    command.orderId(), command.userId(), command.quantity()));
        } catch (Exception ex) {
            log.error("Failed to publish events for reservation {}: {}", saved.getId(), ex.getMessage());
        }

        log.info("Stock reserved: reservationId={} bookId={} orderId={} qty={}",
                saved.getId(), command.bookId(), command.orderId(), command.quantity());
        return saved;
    }
}

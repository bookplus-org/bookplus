package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.AddStockUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class AddStockUseCaseImpl implements AddStockUseCase {

    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Stock addStock(AddStockCommand command) {
        BookId bookId = BookId.of(command.bookId());

        Stock stock = loadStockPort.findByBookId(bookId)
                .orElseThrow(() -> new StockNotFoundException(command.bookId()));

        StockMovement movement = stock.addStock(command.quantity(), command.referenceId(), command.notes());

        Stock saved = saveStockPort.save(stock);
        saveMovementPort.save(movement);

        try {
            eventPublisher.publishAll(saved.pullDomainEvents());
        } catch (Exception ex) {
            log.error("Failed to publish StockUpdatedEvent for book {}: {}", command.bookId(), ex.getMessage());
        }

        log.info("Stock added: bookId={} qty={} total={}", command.bookId(),
                command.quantity(), saved.getQuantityTotal());
        return saved;
    }
}

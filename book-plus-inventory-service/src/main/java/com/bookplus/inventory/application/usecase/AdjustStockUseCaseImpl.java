package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.AdjustStockUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class AdjustStockUseCaseImpl implements AdjustStockUseCase {

    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final SaveMovementPort         saveMovementPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Stock adjust(AdjustStockCommand command) {
        BookId bookId = BookId.of(command.bookId());

        Stock stock = loadStockPort.findByBookId(bookId)
                .orElseThrow(() -> new StockNotFoundException(command.bookId()));

        if (command.lowStockThreshold() >= 0) {
            stock.setLowStockThreshold(command.lowStockThreshold());
        }

        StockMovement movement = stock.adjust(command.newTotalQuantity(), command.notes());
        Stock saved = saveStockPort.save(stock);
        saveMovementPort.save(movement);

        try {
            eventPublisher.publishAll(saved.pullDomainEvents());
        } catch (Exception ex) {
            log.error("Failed to publish events after adjustment for book {}: {}", command.bookId(), ex.getMessage());
        }

        log.info("Stock adjusted: bookId={} newTotal={}", command.bookId(), command.newTotalQuantity());
        return saved;
    }
}

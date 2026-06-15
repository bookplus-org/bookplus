package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.DomainException;
import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.Stock;
import com.bookplus.inventory.domain.port.in.InitializeStockUseCase;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class InitializeStockUseCaseImpl implements InitializeStockUseCase {

    private final LoadStockPort            loadStockPort;
    private final SaveStockPort            saveStockPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Stock initialize(InitializeStockCommand command) {
        BookId bookId = BookId.of(command.bookId());

        if (loadStockPort.existsByBookId(bookId)) {
            throw new DomainException("Stock already exists for book: " + command.bookId());
        }

        Stock stock = Stock.create(bookId, command.initialQuantity(), command.lowStockThreshold());
        Stock saved = saveStockPort.save(stock);

        try {
            eventPublisher.publishAll(saved.pullDomainEvents());
        } catch (Exception ex) {
            log.error("Failed to publish StockCreatedEvent for book {}: {}", command.bookId(), ex.getMessage());
        }

        log.info("Stock initialized: bookId={} qty={}", command.bookId(), command.initialQuantity());
        return saved;
    }
}

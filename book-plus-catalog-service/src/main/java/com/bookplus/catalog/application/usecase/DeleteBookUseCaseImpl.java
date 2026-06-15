package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.port.in.DeleteBookUseCase;
import com.bookplus.catalog.domain.port.out.*;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase @RequiredArgsConstructor @Slf4j
public class DeleteBookUseCaseImpl implements DeleteBookUseCase {

    private final LoadBookPort        loadBookPort;
    private final SaveBookPort        saveBookPort;
    private final IndexBookPort       indexBookPort;
    private final CachePort           cachePort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void delete(String bookId) {
        var book = loadBookPort.findById(BookId.of(bookId))
                .orElseThrow(() -> new BookNotFoundException(bookId));

        book.deactivate();
        saveBookPort.save(book);

        cachePort.evictBook("book:" + bookId);
        indexBookPort.remove(book.getId());
        eventPublisher.publishAll(book.pullDomainEvents());
        log.info("Book soft-deleted: {}", bookId);
    }
}

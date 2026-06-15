package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.UpdateBookUseCase;
import com.bookplus.catalog.domain.port.out.*;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase @RequiredArgsConstructor @Slf4j
public class UpdateBookUseCaseImpl implements UpdateBookUseCase {

    private final LoadBookPort        loadBookPort;
    private final SaveBookPort        saveBookPort;
    private final LoadCategoryPort    loadCategoryPort;
    private final IndexBookPort       indexBookPort;
    private final CachePort           cachePort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Book update(String bookId, UpdateBookCommand cmd) {
        Book book = loadBookPort.findById(BookId.of(bookId))
                .orElseThrow(() -> new BookNotFoundException(bookId));

        CategoryId categoryId = CategoryId.of(cmd.categoryId());
        loadCategoryPort.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotFoundException(cmd.categoryId()));

        Money price = Money.of(cmd.price(), cmd.currency());
        Money discountPrice = cmd.discountPrice() != null
                ? Money.of(cmd.discountPrice(), cmd.currency()) : null;

        book.update(cmd.title(), cmd.author(), cmd.description(), price, discountPrice,
                cmd.imageUrl(), cmd.previewUrl(), cmd.publisher(), cmd.publishedDate(),
                cmd.language(), cmd.pages(), categoryId);

        Book saved = saveBookPort.save(book);
        log.info("Book updated: {}", saved.getId());

        // Invalidar caché y re-indexar
        cachePort.evictBook("book:" + bookId);
        cachePort.evictBook("book:slug:" + saved.getSlug());
        try { indexBookPort.index(saved); }
        catch (Exception ex) { log.warn("ES re-index failed: {}", ex.getMessage()); }

        eventPublisher.publishAll(saved.pullDomainEvents());
        return saved;
    }
}

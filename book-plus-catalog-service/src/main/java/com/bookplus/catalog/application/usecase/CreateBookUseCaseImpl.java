package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookAlreadyExistsException;
import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.CreateBookUseCase;
import com.bookplus.catalog.domain.port.out.*;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@UseCase @RequiredArgsConstructor @Slf4j
public class CreateBookUseCaseImpl implements CreateBookUseCase {

    private final LoadBookPort        loadBookPort;
    private final SaveBookPort        saveBookPort;
    private final LoadCategoryPort    loadCategoryPort;
    private final IndexBookPort       indexBookPort;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Book create(CreateBookCommand cmd) {
        ISBN isbn = ISBN.of(cmd.isbn());

        if (loadBookPort.existsByIsbn(isbn)) {
            throw new BookAlreadyExistsException(cmd.isbn());
        }

        CategoryId categoryId = CategoryId.of(cmd.categoryId());
        loadCategoryPort.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotFoundException(cmd.categoryId()));

        Money price = Money.of(cmd.price(), cmd.currency());
        Money discountPrice = cmd.discountPrice() != null
                ? Money.of(cmd.discountPrice(), cmd.currency()) : null;

        Book book = Book.create(isbn, cmd.title(), cmd.author(), cmd.description(),
                price, cmd.imageUrl(), cmd.previewUrl(), cmd.publisher(),
                cmd.publishedDate(), cmd.language(), cmd.pages(), categoryId);

        if (discountPrice != null) book.applyDiscount(discountPrice);

        Book saved = saveBookPort.save(book);
        log.info("Book created: {} — ISBN: {}", saved.getTitle(), saved.getIsbn());

        // Indexar en Elasticsearch (sync — para que sea buscable inmediatamente)
        try { indexBookPort.index(saved); }
        catch (Exception ex) { log.warn("Elasticsearch indexing failed for book {}: {}", saved.getId(), ex.getMessage()); }

        // Publicar eventos Kafka async
        eventPublisher.publishAll(saved.pullDomainEvents());

        return saved;
    }
}

package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.GetBookUseCase;
import com.bookplus.catalog.domain.port.out.*;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class GetBookUseCaseImpl implements GetBookUseCase {

    private final LoadBookPort loadBookPort;
    private final CachePort    cachePort;

    @Override
    public Book getById(String id) {
        String cacheKey = "book:" + id;
        return cachePort.getBook(cacheKey).orElseGet(() -> {
            Book book = loadBookPort.findById(BookId.of(id))
                    .filter(Book::isActive)
                    .orElseThrow(() -> new BookNotFoundException(id));
            cachePort.putBook(cacheKey, book);
            return book;
        });
    }

    @Override
    public Book getByIsbn(String isbn) {
        return loadBookPort.findByIsbn(ISBN.of(isbn))
                .filter(Book::isActive)
                .orElseThrow(() -> new BookNotFoundException("ISBN:" + isbn));
    }

    @Override
    public Book getBySlug(String slug) {
        String cacheKey = "book:slug:" + slug;
        return cachePort.getBook(cacheKey).orElseGet(() -> {
            Book book = loadBookPort.findBySlug(Slug.of(slug))
                    .filter(Book::isActive)
                    .orElseThrow(() -> new BookNotFoundException("slug:" + slug));
            cachePort.putBook(cacheKey, book);
            return book;
        });
    }
}

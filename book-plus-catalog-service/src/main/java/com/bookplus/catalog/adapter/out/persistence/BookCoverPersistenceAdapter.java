package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.entity.BookCoverEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.BookCoverJpaRepository;
import com.bookplus.catalog.domain.model.BookCover;
import com.bookplus.catalog.domain.port.out.BookCoverPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Implementa {@link BookCoverPort} sobre JPA (tabla book_covers, BYTEA). */
@PersistenceAdapter
@RequiredArgsConstructor
public class BookCoverPersistenceAdapter implements BookCoverPort {

    private final BookCoverJpaRepository repository;

    @Override
    public Optional<BookCover> findByBookId(String bookId) {
        return repository.findById(UUID.fromString(bookId)).map(BookCoverPersistenceAdapter::toDomain);
    }

    @Override
    public void save(BookCover c) {
        repository.save(BookCoverEntity.builder()
                .bookId(UUID.fromString(c.bookId()))
                .image(c.image())
                .contentType(c.contentType())
                .updatedAt(c.updatedAt() != null ? c.updatedAt() : Instant.now())
                .build());
    }

    private static BookCover toDomain(BookCoverEntity e) {
        return new BookCover(e.getBookId().toString(), e.getImage(), e.getContentType(), e.getUpdatedAt());
    }
}

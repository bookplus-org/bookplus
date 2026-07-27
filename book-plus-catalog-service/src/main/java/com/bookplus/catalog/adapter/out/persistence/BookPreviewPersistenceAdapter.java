package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.entity.BookPreviewEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.BookPreviewJpaRepository;
import com.bookplus.catalog.domain.model.BookPreview;
import com.bookplus.catalog.domain.port.out.BookPreviewPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Implementa {@link BookPreviewPort} sobre JPA (tabla book_previews, BYTEA). */
@PersistenceAdapter
@RequiredArgsConstructor
public class BookPreviewPersistenceAdapter implements BookPreviewPort {

    private final BookPreviewJpaRepository repository;

    @Override
    public Optional<BookPreview> findByBookId(String bookId) {
        return repository.findById(UUID.fromString(bookId)).map(BookPreviewPersistenceAdapter::toDomain);
    }

    @Override
    public void save(BookPreview p) {
        repository.save(BookPreviewEntity.builder()
                .bookId(UUID.fromString(p.bookId()))
                .previewPdf(p.previewPdf())
                .pageCount(p.pageCount())
                .sourcePages(p.sourcePages())
                .fullPdf(p.fullPdf())
                .fullPages(p.fullPages())
                .updatedAt(p.updatedAt() != null ? p.updatedAt() : Instant.now())
                .build());
    }

    private static BookPreview toDomain(BookPreviewEntity e) {
        return new BookPreview(
                e.getBookId().toString(),
                e.getPreviewPdf(), e.getPageCount(), e.getSourcePages(),
                e.getFullPdf(), e.getFullPages(), e.getUpdatedAt());
    }
}

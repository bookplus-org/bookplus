package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.mapper.BookPersistenceMapper;
import com.bookplus.catalog.adapter.out.persistence.repository.BookJpaRepository;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.ListBooksUseCase.ListBooksQuery;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.domain.port.out.SaveBookPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
@RequiredArgsConstructor
public class BookPersistenceAdapter implements LoadBookPort, SaveBookPort {

    private final BookJpaRepository    repository;
    private final BookPersistenceMapper mapper;

    // ── LoadBookPort ──────────────────────────────────────────────────────

    @Override
    public Optional<Book> findById(BookId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Book> findByIsbn(ISBN isbn) {
        return repository.findByIsbn(isbn.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Book> findBySlug(Slug slug) {
        return repository.findBySlug(slug.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIsbn(ISBN isbn) {
        return repository.existsByIsbn(isbn.value());
    }

    @Override
    public PagedResult<Book> findAll(ListBooksQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(query.page(), query.size(), sort);

        UUID categoryUuid = query.categoryId() != null
                ? CategoryId.of(query.categoryId()).value() : null;

        Page<?> page;
        if (categoryUuid != null && query.author() != null) {
            page = repository.findAllByCategoryIdAndAuthorContainingIgnoreCaseAndActiveTrue(
                    categoryUuid, query.author(), pageable);
        } else if (categoryUuid != null) {
            page = repository.findAllByCategoryIdAndActiveTrue(categoryUuid, pageable);
        } else if (query.author() != null) {
            page = repository.findAllByAuthorContainingIgnoreCaseAndActiveTrue(
                    query.author(), pageable);
        } else {
            page = repository.findAllByActiveTrue(pageable);
        }

        // Re-cast — all page variants return BookEntity
        @SuppressWarnings("unchecked")
        Page<com.bookplus.catalog.adapter.out.persistence.entity.BookEntity> bookPage =
                (Page<com.bookplus.catalog.adapter.out.persistence.entity.BookEntity>) page;

        return new PagedResult<>(
                bookPage.getContent().stream().map(mapper::toDomain).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                bookPage.isFirst(),
                bookPage.isLast()
        );
    }

    // ── SaveBookPort ──────────────────────────────────────────────────────

    @Override
    public Book save(Book book) {
        return mapper.toDomain(repository.save(mapper.toEntity(book)));
    }
}

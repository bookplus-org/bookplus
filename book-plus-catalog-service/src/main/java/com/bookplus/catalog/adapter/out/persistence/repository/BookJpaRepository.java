package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookJpaRepository extends JpaRepository<BookEntity, UUID> {

    Optional<BookEntity> findByIsbn(String isbn);

    Optional<BookEntity> findBySlug(String slug);

    boolean existsByIsbn(String isbn);

    Page<BookEntity> findAllByActiveTrue(Pageable pageable);

    Page<BookEntity> findAllByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);

    Page<BookEntity> findAllByAuthorContainingIgnoreCaseAndActiveTrue(String author, Pageable pageable);

    Page<BookEntity> findAllByCategoryIdAndAuthorContainingIgnoreCaseAndActiveTrue(
            UUID categoryId, String author, Pageable pageable);
}

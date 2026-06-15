package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    Page<ReviewEntity> findAllByBookId(UUID bookId, Pageable pageable);

    boolean existsByBookIdAndUserId(UUID bookId, String userId);
}

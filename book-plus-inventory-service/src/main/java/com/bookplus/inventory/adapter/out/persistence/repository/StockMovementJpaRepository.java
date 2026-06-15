package com.bookplus.inventory.adapter.out.persistence.repository;

import com.bookplus.inventory.adapter.out.persistence.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, UUID> {
    Page<StockMovementEntity> findAllByBookId(UUID bookId, Pageable pageable);
}

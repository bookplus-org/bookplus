package com.bookplus.inventory.adapter.out.persistence.repository;

import com.bookplus.inventory.adapter.out.persistence.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<StockEntity, UUID> {
    Optional<StockEntity> findByBookId(UUID bookId);
    boolean existsByBookId(UUID bookId);
}

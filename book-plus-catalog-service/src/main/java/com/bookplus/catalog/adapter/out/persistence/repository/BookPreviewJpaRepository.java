package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.BookPreviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookPreviewJpaRepository extends JpaRepository<BookPreviewEntity, UUID> {
}

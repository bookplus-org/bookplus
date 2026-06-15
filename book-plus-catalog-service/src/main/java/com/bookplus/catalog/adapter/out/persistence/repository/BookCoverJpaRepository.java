package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.BookCoverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookCoverJpaRepository extends JpaRepository<BookCoverEntity, UUID> {
}

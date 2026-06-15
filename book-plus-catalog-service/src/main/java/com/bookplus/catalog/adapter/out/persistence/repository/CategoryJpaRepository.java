package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<CategoryEntity> findAllByActiveTrue();

    List<CategoryEntity> findAllByParentId(UUID parentId);
}

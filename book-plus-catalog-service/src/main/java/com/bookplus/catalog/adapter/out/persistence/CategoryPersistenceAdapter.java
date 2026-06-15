package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.mapper.CategoryPersistenceMapper;
import com.bookplus.catalog.adapter.out.persistence.repository.CategoryJpaRepository;
import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;
import com.bookplus.catalog.domain.port.out.LoadCategoryPort;
import com.bookplus.catalog.domain.port.out.SaveCategoryPort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements LoadCategoryPort, SaveCategoryPort {

    private final CategoryJpaRepository    repository;
    private final CategoryPersistenceMapper mapper;

    // ── LoadCategoryPort ──────────────────────────────────────────────────

    @Override
    public Optional<Category> findById(CategoryId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return repository.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public List<Category> findAllActive() {
        return repository.findAllByActiveTrue().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Category> findByParentId(CategoryId parentId) {
        return repository.findAllByParentId(parentId.value()).stream()
                .map(mapper::toDomain).toList();
    }

    // ── SaveCategoryPort ──────────────────────────────────────────────────

    @Override
    public Category save(Category category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }
}

package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;

import java.util.List;
import java.util.Optional;

public interface LoadCategoryPort {
    Optional<Category> findById(CategoryId id);
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    List<Category> findAllActive();
    List<Category> findByParentId(CategoryId parentId);
}

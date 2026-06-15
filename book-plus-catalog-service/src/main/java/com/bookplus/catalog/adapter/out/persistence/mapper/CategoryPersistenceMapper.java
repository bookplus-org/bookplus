package com.bookplus.catalog.adapter.out.persistence.mapper;

import com.bookplus.catalog.adapter.out.persistence.entity.CategoryEntity;
import com.bookplus.catalog.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public Category toDomain(CategoryEntity e) {
        return Category.reconstitute(
                CategoryId.of(e.getId()),
                e.getName(),
                Slug.of(e.getSlug()),
                e.getDescription(),
                e.getParentId() != null ? CategoryId.of(e.getParentId()) : null,
                e.getImageUrl(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public CategoryEntity toEntity(Category c) {
        return CategoryEntity.builder()
                .id(c.getId().value())
                .name(c.getName())
                .slug(c.getSlug().value())
                .description(c.getDescription())
                .parentId(c.getParentId() != null ? c.getParentId().value() : null)
                .imageUrl(c.getImageUrl())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

package com.bookplus.catalog.adapter.in.web.dto;

import com.bookplus.catalog.domain.model.Category;

import java.time.Instant;

public record CategoryResponse(
        String  id,
        String  name,
        String  slug,
        String  description,
        String  parentId,
        String  imageUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId().value().toString(),
                c.getName(),
                c.getSlug().value(),
                c.getDescription(),
                c.getParentId() != null ? c.getParentId().value().toString() : null,
                c.getImageUrl(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}

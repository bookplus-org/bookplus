package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Category;

public interface UpdateCategoryUseCase {
    Category update(String categoryId, UpdateCategoryCommand command);
    record UpdateCategoryCommand(String name, String description, String imageUrl) {}
}

package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Category;

/** Puerto de entrada — crear categoría (ADMIN). */
public interface CreateCategoryUseCase {
    Category create(CreateCategoryCommand command);

    record CreateCategoryCommand(
            String name, String description, String parentId, String imageUrl
    ) {}
}

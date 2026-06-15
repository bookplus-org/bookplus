package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Category;

public interface SaveCategoryPort {
    Category save(Category category);
}

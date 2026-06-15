package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Category;
import java.util.List;

/** Puerto de entrada — listar categorías activas. */
public interface ListCategoriesUseCase {
    List<Category> listAll();
    List<Category> listByParent(String parentId);
}

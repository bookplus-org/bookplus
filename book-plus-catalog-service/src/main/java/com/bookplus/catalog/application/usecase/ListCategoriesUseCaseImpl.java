package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;
import com.bookplus.catalog.domain.port.in.ListCategoriesUseCase;
import com.bookplus.catalog.domain.port.out.LoadCategoryPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {

    private final LoadCategoryPort loadCategoryPort;

    @Override
    public List<Category> listAll() {
        log.debug("Listing all active categories");
        return loadCategoryPort.findAllActive();
    }

    @Override
    public List<Category> listByParent(String parentId) {
        log.debug("Listing categories by parent: id='{}'", parentId);
        return loadCategoryPort.findByParentId(CategoryId.of(parentId));
    }
}

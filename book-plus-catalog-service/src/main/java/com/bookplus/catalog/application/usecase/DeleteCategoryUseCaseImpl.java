package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;
import com.bookplus.catalog.domain.port.in.DeleteCategoryUseCase;
import com.bookplus.catalog.domain.port.out.LoadCategoryPort;
import com.bookplus.catalog.domain.port.out.SaveCategoryPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class DeleteCategoryUseCaseImpl implements DeleteCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    @Override
    public void delete(String categoryId) {
        log.debug("Deleting category: id='{}'", categoryId);

        CategoryId id = CategoryId.of(categoryId);

        Category category = loadCategoryPort.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        // Guard: cannot deactivate a category that has active children
        List<Category> children = loadCategoryPort.findByParentId(id);
        boolean hasActiveChildren = children.stream().anyMatch(Category::isActive);
        if (hasActiveChildren) {
            throw new com.bookplus.catalog.domain.exception.DomainException(
                    "Cannot deactivate category with active sub-categories: " + categoryId);
        }

        category.deactivate();
        saveCategoryPort.save(category);

        log.info("Category deactivated (soft-delete): id={}", categoryId);
    }
}

package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;
import com.bookplus.catalog.domain.port.in.UpdateCategoryUseCase;
import com.bookplus.catalog.domain.port.out.LoadCategoryPort;
import com.bookplus.catalog.domain.port.out.SaveCategoryPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateCategoryUseCaseImpl implements UpdateCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    @Override
    public Category update(String categoryId, UpdateCategoryCommand command) {
        log.debug("Updating category: id='{}'", categoryId);

        Category category = loadCategoryPort.findById(CategoryId.of(categoryId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        // Name uniqueness check — skip if name did not change
        if (!category.getName().equalsIgnoreCase(command.name())) {
            loadCategoryPort.findByName(command.name()).ifPresent(existing -> {
                throw new com.bookplus.catalog.domain.exception.DomainException(
                        "Category name already in use: " + command.name());
            });
        }

        category.update(command.name(), command.description(), command.imageUrl());

        Category saved = saveCategoryPort.save(category);
        log.info("Category updated: id={} name='{}'", saved.getId().value(), saved.getName());
        return saved;
    }
}

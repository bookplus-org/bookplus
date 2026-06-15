package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.CategoryAlreadyExistsException;
import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.model.CategoryId;
import com.bookplus.catalog.domain.port.in.CreateCategoryUseCase;
import com.bookplus.catalog.domain.port.out.LoadCategoryPort;
import com.bookplus.catalog.domain.port.out.SaveCategoryPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    @Override
    public Category create(CreateCategoryCommand command) {
        log.debug("Creating category: name='{}'", command.name());

        // Guard: name uniqueness
        if (loadCategoryPort.existsByName(command.name())) {
            throw new CategoryAlreadyExistsException(command.name());
        }

        // Validate parent if provided
        CategoryId parentId = null;
        if (command.parentId() != null && !command.parentId().isBlank()) {
            parentId = CategoryId.of(command.parentId());
            // Ensure parent exists and is active
            Category parent = loadCategoryPort.findById(parentId)
                    .orElseThrow(() -> new CategoryNotFoundException(command.parentId()));
            if (!parent.isActive()) {
                throw new com.bookplus.catalog.domain.exception.DomainException(
                        "Parent category is inactive: " + command.parentId());
            }
        }

        Category category = Category.create(
                command.name(),
                command.description(),
                parentId,
                command.imageUrl()
        );

        Category saved = saveCategoryPort.save(category);
        log.info("Category created: id={} name='{}'", saved.getId().value(), saved.getName());
        return saved;
    }
}

package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.*;
import com.bookplus.catalog.domain.model.Category;
import com.bookplus.catalog.domain.port.in.*;
import com.bookplus.catalog.domain.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.bookplus.catalog.domain.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Adapter IN — Endpoints de administración de categorías.
 * Requiere rol ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
@Tag(name = "Admin Categories", description = "Admin category management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    // ── POST /api/v1/admin/categories ───────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request) {

        CreateCategoryCommand command = new CreateCategoryCommand(
                request.name(),
                request.description(),
                request.parentId(),
                request.imageUrl()
        );

        Category category = createCategoryUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(CategoryResponse.from(category)));
    }

    // ── PUT /api/v1/admin/categories/{id} ──────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest request) {

        UpdateCategoryCommand command = new UpdateCategoryCommand(
                request.name(),
                request.description(),
                request.imageUrl()
        );

        Category category = updateCategoryUseCase.update(id, command);
        return ResponseEntity.ok(ApiResponse.ok(CategoryResponse.from(category)));
    }

    // ── DELETE /api/v1/admin/categories/{id} ────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate (soft-delete) a category")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteCategoryUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

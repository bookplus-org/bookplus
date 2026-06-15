package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.CategoryResponse;
import com.bookplus.catalog.domain.port.in.ListCategoriesUseCase;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adapter IN — Controlador público de categorías.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public category endpoints")
public class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;

    // ── GET /api/v1/categories ──────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listAll() {
        List<CategoryResponse> response = listCategoriesUseCase.listAll()
                .stream().map(CategoryResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── GET /api/v1/categories/{parentId}/children ──────────────────────────

    @GetMapping("/{parentId}/children")
    @Operation(summary = "List sub-categories of a parent")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listByParent(
            @PathVariable String parentId) {
        List<CategoryResponse> response = listCategoriesUseCase.listByParent(parentId)
                .stream().map(CategoryResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.*;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.*;
import com.bookplus.catalog.domain.port.in.CreateBookUseCase.CreateBookCommand;
import com.bookplus.catalog.domain.port.in.UpdateBookUseCase.UpdateBookCommand;
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
 * Adapter IN — Endpoints de administración de libros.
 * Requiere rol ROLE_EDITOR o ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPERADMIN')")
@Tag(name = "Admin Books", description = "Admin book management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookController {

    private final CreateBookUseCase createBookUseCase;
    private final UpdateBookUseCase updateBookUseCase;
    private final DeleteBookUseCase deleteBookUseCase;

    // ── POST /api/v1/admin/books ────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new book")
    public ResponseEntity<ApiResponse<BookResponse>> create(
            @Valid @RequestBody CreateBookRequest request) {

        CreateBookCommand command = new CreateBookCommand(
                request.isbn(),
                request.title(),
                request.author(),
                request.description(),
                request.price(),
                request.currency(),
                null,                  // discountPrice — se aplica vía update/descuento
                request.imageUrl(),
                request.previewUrl(),
                request.publisher(),
                request.publishedDate(),
                request.language(),
                request.pages(),
                request.categoryId()
        );

        Book book = createBookUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BookResponse.from(book)));
    }

    // ── PUT /api/v1/admin/books/{id} ────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing book")
    public ResponseEntity<ApiResponse<BookResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookRequest request) {

        UpdateBookCommand command = new UpdateBookCommand(
                request.title(),
                request.author(),
                request.description(),
                request.price(),
                request.currency(),
                request.discountPrice(),
                request.imageUrl(),
                request.previewUrl(),
                request.publisher(),
                request.publishedDate(),
                request.language(),
                request.pages(),
                request.categoryId()
        );

        Book book = updateBookUseCase.update(id, command);
        return ResponseEntity.ok(ApiResponse.ok(BookResponse.from(book)));
    }

    // ── DELETE /api/v1/admin/books/{id} ────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a book")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteBookUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

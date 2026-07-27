package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.BookResponse;
import com.bookplus.catalog.domain.port.in.ManageFavoritesUseCase;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Favoritos / lista de deseos del usuario autenticado.
 * Ruta en el gateway: /api/v1/favorites/** → catalog-service (requiere auth).
 *
 * Controlador delgado: delega toda la lógica en {@link ManageFavoritesUseCase}.
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "User wishlist")
@SecurityRequirement(name = "bearerAuth")
public class FavoritesController {

    private final ManageFavoritesUseCase manageFavoritesUseCase;

    @GetMapping
    @Operation(summary = "List my favorite books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> myFavorites(@AuthenticationPrincipal Jwt jwt) {
        List<BookResponse> books = manageFavoritesUseCase.listFavoriteBooks(jwt.getSubject())
                .stream().map(BookResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(books));
    }

    @GetMapping("/ids")
    @Operation(summary = "List my favorite book ids")
    public ResponseEntity<ApiResponse<List<String>>> myFavoriteIds(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(manageFavoritesUseCase.listFavoriteIds(jwt.getSubject())));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "Add a book to favorites (idempotent)")
    public ResponseEntity<ApiResponse<Void>> add(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        manageFavoritesUseCase.add(jwt.getSubject(), bookId.toString());
        return ResponseEntity.ok(ApiResponse.ok("Añadido a favoritos"));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Remove a book from favorites")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        manageFavoritesUseCase.remove(jwt.getSubject(), bookId.toString());
        return ResponseEntity.ok(ApiResponse.ok("Eliminado de favoritos"));
    }
}

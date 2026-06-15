package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.BookResponse;
import com.bookplus.catalog.adapter.out.persistence.entity.UserFavoriteEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.UserFavoriteJpaRepository;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Favoritos / lista de deseos del usuario autenticado.
 * Ruta en el gateway: /api/v1/favorites/** → catalog-service (requiere auth).
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "User wishlist")
@SecurityRequirement(name = "bearerAuth")
public class FavoritesController {

    private final UserFavoriteJpaRepository favoriteRepo;
    private final LoadBookPort              loadBookPort;

    @GetMapping
    @Operation(summary = "List my favorite books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> myFavorites(@AuthenticationPrincipal Jwt jwt) {
        List<BookResponse> books = favoriteRepo.findByUserIdOrderByCreatedAtDesc(jwt.getSubject())
                .stream()
                .map(fav -> loadBookPort.findById(BookId.of(fav.getBookId().toString())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(BookResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(books));
    }

    @GetMapping("/ids")
    @Operation(summary = "List my favorite book ids")
    public ResponseEntity<ApiResponse<List<String>>> myFavoriteIds(@AuthenticationPrincipal Jwt jwt) {
        List<String> ids = favoriteRepo.findByUserIdOrderByCreatedAtDesc(jwt.getSubject())
                .stream().map(f -> f.getBookId().toString()).toList();
        return ResponseEntity.ok(ApiResponse.ok(ids));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "Add a book to favorites (idempotent)")
    public ResponseEntity<ApiResponse<Void>> add(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        if (!favoriteRepo.existsByUserIdAndBookId(jwt.getSubject(), bookId)) {
            favoriteRepo.save(UserFavoriteEntity.builder()
                    .userId(jwt.getSubject()).bookId(bookId).createdAt(Instant.now()).build());
        }
        return ResponseEntity.ok(ApiResponse.ok("Añadido a favoritos"));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Remove a book from favorites")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        favoriteRepo.deleteByUserIdAndBookId(jwt.getSubject(), bookId);
        return ResponseEntity.ok(ApiResponse.ok("Eliminado de favoritos"));
    }
}

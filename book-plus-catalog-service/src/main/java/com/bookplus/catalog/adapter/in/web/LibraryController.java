package com.bookplus.catalog.adapter.in.web;

import com.bookplus.catalog.adapter.in.web.dto.BookResponse;
import com.bookplus.catalog.domain.port.in.LibraryUseCase;
import com.bookplus.catalog.domain.port.in.LibraryUseCase.ConsumptionFacts;
import com.bookplus.catalog.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Biblioteca del usuario: libros comprados (acceso al PDF completo).
 * Ruta en el gateway: /api/v1/library/** → catalog-service (requiere auth).
 *
 * Controlador delgado: resuelve el JWT/roles (seguridad) y delega en {@link LibraryUseCase}.
 */
@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
@Tag(name = "Library", description = "Purchased books & downloads")
@SecurityRequirement(name = "bearerAuth")
public class LibraryController {

    private final LibraryUseCase libraryUseCase;

    @GetMapping
    @Operation(summary = "List books I purchased")
    public ResponseEntity<ApiResponse<List<BookResponse>>> myLibrary(@AuthenticationPrincipal Jwt jwt) {
        List<BookResponse> books = libraryUseCase.listPurchasedBooks(jwt.getSubject())
                .stream().map(BookResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(books));
    }

    @GetMapping("/{bookId}/book.pdf")
    @Operation(summary = "Download/read the full PDF of a purchased book (or admin)")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {

        return libraryUseCase.downloadFullPdf(jwt.getSubject(), bookId.toString(), hasAdminRole(jwt))
                .map(pdf -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"book.pdf\"")
                        .header("X-Total-Pages",
                                pdf.totalPages() == null ? "" : String.valueOf(pdf.totalPages()))
                        .cacheControl(CacheControl.noCache())
                        .body(pdf.bytes()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{bookId}/progress")
    @Operation(summary = "Report reading progress (0-100) for a purchased book")
    public ResponseEntity<ApiResponse<Integer>> updateProgress(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId,
            @RequestParam("percent") int percent) {

        int progress = libraryUseCase.updateProgress(jwt.getSubject(), bookId.toString(), percent);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    @GetMapping("/admin/consumption")
    @Operation(summary = "[ADMIN] Read a user's consumption facts for a book (refund policy input)")
    public ResponseEntity<ApiResponse<ConsumptionResponse>> consumption(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String userId,
            @RequestParam UUID bookId) {

        if (!hasAdminRole(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo administración");
        }
        ConsumptionFacts f = libraryUseCase.getConsumption(userId, bookId.toString());
        return ResponseEntity.ok(ApiResponse.ok(
                new ConsumptionResponse(f.downloaded(), f.readProgress(), f.active())));
    }

    /** Hechos de consumo de un libro por un usuario, para la política de reembolsos. */
    public record ConsumptionResponse(boolean downloaded, int readProgress, boolean active) {}

    private boolean hasAdminRole(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof List<?> list) {
            for (Object r : list) {
                String s = String.valueOf(r);
                if ("ROLE_EDITOR".equals(s) || "ROLE_ADMIN".equals(s) || "ROLE_SUPERADMIN".equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}

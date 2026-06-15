package com.bookplus.catalog.adapter.in.web.preview;

import com.bookplus.catalog.adapter.out.persistence.entity.BookPreviewEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints del visor de libro:
 *   - público:  GET /api/v1/books/{id}/preview.pdf  → muestra (primeras N páginas)
 *   - admin:    POST /api/v1/admin/books/{id}/preview (multipart "file") → sube PDF
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Book Preview", description = "PDF sample viewer")
public class BookPreviewController {

    private final PdfPreviewService previewService;

    // ── Público: servir la muestra ─────────────────────────────────────────
    @GetMapping("/api/v1/books/{id}/preview.pdf")
    @Operation(summary = "Get the book's PDF sample (first pages only). 204 if none.")
    public ResponseEntity<byte[]> getPreview(@PathVariable UUID id) {
        // 204 (no toast on the client) when the book has no uploaded sample.
        return previewService.getPreview(id)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                        .header("X-Preview-Pages", String.valueOf(p.getPageCount()))
                        .header("X-Total-Pages",
                                p.getSourcePages() == null ? "" : String.valueOf(p.getSourcePages()))
                        .cacheControl(CacheControl.noCache())
                        .body(p.getPreviewPdf()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ── Admin: ver el PDF completo ─────────────────────────────────────────
    @GetMapping("/api/v1/admin/books/{id}/full.pdf")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPERADMIN')")
    @Operation(summary = "Admin-only: get the full uploaded PDF. 204 if none.")
    public ResponseEntity<byte[]> getFull(@PathVariable UUID id) {
        return previewService.getPreview(id)
                .filter(p -> p.getFullPdf() != null)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"book.pdf\"")
                        .header("X-Total-Pages",
                                p.getFullPages() == null ? "" : String.valueOf(p.getFullPages()))
                        .cacheControl(CacheControl.noCache())
                        .body(p.getFullPdf()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ── Admin: subir el PDF (se guarda muestra + completo) ─────────────────
    @PostMapping(path = "/api/v1/admin/books/{id}/preview",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a book PDF; only the first pages are kept as the sample")
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo requerido");
        }
        String ct = file.getContentType();
        if (ct != null && !ct.toLowerCase().contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Debe ser un PDF");
        }
        try {
            int pages = previewService.storePreview(id, file.getBytes());
            return ResponseEntity.ok(Map.of("bookId", id, "previewPages", pages));
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
        }
    }
}

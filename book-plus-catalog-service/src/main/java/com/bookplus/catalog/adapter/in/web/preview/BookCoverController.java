package com.bookplus.catalog.adapter.in.web.preview;

import com.bookplus.catalog.adapter.out.persistence.entity.BookCoverEntity;
import com.bookplus.catalog.adapter.out.persistence.entity.BookEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.BookCoverJpaRepository;
import com.bookplus.catalog.adapter.out.persistence.repository.BookJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Portada del libro:
 *   - público:  GET  /api/v1/books/{id}/cover
 *   - admin:    POST /api/v1/admin/books/{id}/cover (multipart "file")
 *
 * Al subir, books.image_url apunta al endpoint de portada.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Book Cover", description = "Book cover image upload/serve")
public class BookCoverController {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final BookCoverJpaRepository coverRepo;
    private final BookJpaRepository      bookRepo;

    @GetMapping("/api/v1/books/{id}/cover")
    @Operation(summary = "Get the book cover image. 204 if none.")
    public ResponseEntity<byte[]> getCover(@PathVariable UUID id) {
        return coverRepo.findById(id)
                .map(c -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(c.getContentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(c.getImage()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(path = "/api/v1/admin/books/{id}/cover",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a book cover image; sets image_url to the cover endpoint")
    @Transactional
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo requerido");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Debe ser una imagen");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 5 MB");
        }

        BookEntity book = bookRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));

        try {
            coverRepo.save(BookCoverEntity.builder()
                    .bookId(id)
                    .image(file.getBytes())
                    .contentType(ct)
                    .updatedAt(Instant.now())
                    .build());
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
        }

        String imageUrl = "/api/v1/books/" + id + "/cover";
        book.setImageUrl(imageUrl);
        bookRepo.save(book);

        return ResponseEntity.ok(Map.of("bookId", id, "imageUrl", imageUrl));
    }
}

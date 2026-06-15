package com.bookplus.catalog.adapter.in.web.preview;

import com.bookplus.catalog.adapter.out.persistence.entity.BookPreviewEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.BookJpaRepository;
import com.bookplus.catalog.adapter.out.persistence.repository.BookPreviewJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

/**
 * Genera y sirve la muestra en PDF de un libro: solo las primeras
 * {@value #MAX_PREVIEW_PAGES} páginas. El PDF completo nunca se almacena.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfPreviewService {

    private static final int MAX_PREVIEW_PAGES = 12;

    private final BookPreviewJpaRepository previewRepo;
    private final BookJpaRepository        bookRepo;

    /** Procesa el PDF subido y persiste solo la muestra (primeras N páginas). */
    public int storePreview(UUID bookId, byte[] pdfBytes) {
        if (!bookRepo.existsById(bookId)) {
            throw new ResponseStatusException(NOT_FOUND, "Libro no encontrado: " + bookId);
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "El archivo PDF está vacío");
        }

        try (PDDocument source = Loader.loadPDF(pdfBytes)) {
            int sourcePages = source.getNumberOfPages();
            if (sourcePages == 0) {
                throw new ResponseStatusException(BAD_REQUEST, "El PDF no tiene páginas");
            }
            int take = Math.min(MAX_PREVIEW_PAGES, sourcePages);

            byte[] sample;
            try (PDDocument out = new PDDocument();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                for (int i = 0; i < take; i++) {
                    out.importPage(source.getPage(i));
                }
                out.save(baos);
                sample = baos.toByteArray();
            }

            previewRepo.save(BookPreviewEntity.builder()
                    .bookId(bookId)
                    .previewPdf(sample)
                    .pageCount(take)
                    .sourcePages(sourcePages)
                    .fullPdf(pdfBytes)        // PDF completo para consulta admin
                    .fullPages(sourcePages)
                    .updatedAt(Instant.now())
                    .build());

            log.info("Preview generado para libro {}: {}/{} páginas", bookId, take, sourcePages);
            return take;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("No se pudo procesar el PDF para libro {}: {}", bookId, ex.getMessage());
            throw new ResponseStatusException(BAD_REQUEST, "Archivo PDF inválido o dañado");
        }
    }

    public Optional<BookPreviewEntity> getPreview(UUID bookId) {
        return previewRepo.findById(bookId);
    }
}

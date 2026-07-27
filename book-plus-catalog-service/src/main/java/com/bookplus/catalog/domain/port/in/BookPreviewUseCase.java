package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.BookPreview;

import java.util.Optional;

/** Casos de uso de la muestra en PDF del libro (generar y servir). */
public interface BookPreviewUseCase {

    /**
     * Procesa el PDF subido: guarda solo las primeras páginas como muestra (y el PDF completo
     * para admin/biblioteca). Devuelve el número de páginas de la muestra.
     */
    int storePreview(String bookId, byte[] pdfBytes);

    Optional<BookPreview> getPreview(String bookId);
}

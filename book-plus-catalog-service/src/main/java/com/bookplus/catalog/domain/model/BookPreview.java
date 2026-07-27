package com.bookplus.catalog.domain.model;

import java.time.Instant;

/**
 * Muestra en PDF de un libro (primeras páginas) más el PDF completo (para consulta admin
 * y descarga de la biblioteca). Vista de dominio: el mapeo BYTEA vive en el adaptador.
 */
public record BookPreview(
        String  bookId,
        byte[]  previewPdf,
        int     pageCount,
        Integer sourcePages,
        byte[]  fullPdf,
        Integer fullPages,
        Instant updatedAt
) {
    public boolean hasPreview() { return previewPdf != null && previewPdf.length > 0; }
    public boolean hasFullPdf() { return fullPdf != null && fullPdf.length > 0; }
}

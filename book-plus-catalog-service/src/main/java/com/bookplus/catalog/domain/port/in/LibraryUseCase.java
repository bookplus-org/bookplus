package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

import java.util.List;
import java.util.Optional;

/** Biblioteca del usuario: libros comprados, descarga del PDF, progreso y hechos de consumo. */
public interface LibraryUseCase {

    /** Libros con acceso vigente del usuario. */
    List<Book> listPurchasedBooks(String userId);

    /**
     * Descarga/lee el PDF completo de un libro comprado (o admin). Marca la descarga.
     * Lanza PurchaseAccessDeniedException si no hay acceso. Vacío si no hay PDF almacenado.
     */
    Optional<PdfContent> downloadFullPdf(String userId, String bookId, boolean isAdmin);

    /** Reporta progreso de lectura (0-100); solo avanza. Devuelve el progreso resultante. */
    int updateProgress(String userId, String bookId, int percent);

    /** Hechos de consumo para la política de reembolsos (uso admin). */
    ConsumptionFacts getConsumption(String userId, String bookId);

    record PdfContent(byte[] bytes, Integer totalPages) {}

    record ConsumptionFacts(boolean downloaded, int readProgress, boolean active) {}
}

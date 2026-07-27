package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.BookCover;

import java.util.Optional;

/** Casos de uso de la portada del libro (subir y servir). */
public interface BookCoverUseCase {

    Optional<BookCover> getCover(String bookId);

    /**
     * Guarda la portada y apunta la image_url del libro al endpoint de portada.
     * Devuelve la URL asignada.
     */
    String uploadCover(String bookId, byte[] image, String contentType);
}

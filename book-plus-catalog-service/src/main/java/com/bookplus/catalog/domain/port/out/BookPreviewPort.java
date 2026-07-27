package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.BookPreview;

import java.util.Optional;

/** Puerto de salida para la muestra/PDF de un libro. */
public interface BookPreviewPort {

    Optional<BookPreview> findByBookId(String bookId);

    void save(BookPreview preview);
}

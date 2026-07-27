package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.BookCover;

import java.util.Optional;

/** Puerto de salida para la portada del libro. */
public interface BookCoverPort {

    Optional<BookCover> findByBookId(String bookId);

    void save(BookCover cover);
}

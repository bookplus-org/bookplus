package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

/** Puerto de entrada — obtener un libro por id o slug. */
public interface GetBookUseCase {
    Book getById(String id);
    Book getByIsbn(String isbn);
    Book getBySlug(String slug);
}

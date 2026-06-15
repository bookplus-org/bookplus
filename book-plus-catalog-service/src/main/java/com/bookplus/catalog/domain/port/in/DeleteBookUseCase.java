package com.bookplus.catalog.domain.port.in;

/** Puerto de entrada — eliminar (soft-delete) libro (ADMIN). */
public interface DeleteBookUseCase {
    void delete(String bookId);
}

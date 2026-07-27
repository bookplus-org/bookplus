package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

import java.util.List;

/** Casos de uso de la lista de favoritos del usuario autenticado. */
public interface ManageFavoritesUseCase {

    /** Libros favoritos (resueltos contra el catálogo, omitiendo los que ya no existen). */
    List<Book> listFavoriteBooks(String userId);

    /** Solo los ids de los favoritos. */
    List<String> listFavoriteIds(String userId);

    /** Añade a favoritos (idempotente). */
    void add(String userId, String bookId);

    /** Quita de favoritos. */
    void remove(String userId, String bookId);
}

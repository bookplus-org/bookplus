package com.bookplus.catalog.domain.port.out;

import java.util.List;

/** Puerto de salida para la lista de favoritos (wishlist) del usuario. */
public interface FavoritePort {

    /** Ids de libros favoritos del usuario, del más reciente al más antiguo. */
    List<String> findBookIdsByUser(String userId);

    boolean exists(String userId, String bookId);

    /** Alta idempotente de un favorito. */
    void add(String userId, String bookId);

    void remove(String userId, String bookId);
}

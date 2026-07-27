package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Purchase;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para las compras del usuario (acceso a la biblioteca / PDF completo). */
public interface PurchasePort {

    /** Ids de libros con acceso vigente, del más reciente al más antiguo. */
    List<String> findActiveBookIds(String userId);

    Optional<Purchase> find(String userId, String bookId);

    boolean hasActiveAccess(String userId, String bookId);

    /** Alta o actualización de una compra (upsert por user_id + book_id). */
    void save(Purchase purchase);
}

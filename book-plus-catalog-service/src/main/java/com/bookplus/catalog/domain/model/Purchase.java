package com.bookplus.catalog.domain.model;

import java.time.Instant;

/**
 * Compra de un usuario (proyección de pedidos confirmados). Concede acceso al PDF completo.
 * Inmutable: los cambios de estado devuelven una copia.
 */
public record Purchase(
        String  userId,
        String  bookId,
        Instant purchasedAt,
        boolean active,
        boolean downloaded,
        int     readProgress
) {
    public Purchase withActive(boolean newActive) {
        return new Purchase(userId, bookId, purchasedAt, newActive, downloaded, readProgress);
    }

    public Purchase withDownloaded(boolean newDownloaded) {
        return new Purchase(userId, bookId, purchasedAt, active, newDownloaded, readProgress);
    }

    public Purchase withReadProgress(int newProgress) {
        return new Purchase(userId, bookId, purchasedAt, active, true, newProgress);
    }
}

package com.bookplus.catalog.domain.port.in;

/**
 * Concesión y revocación de acceso a la biblioteca a partir de eventos de pedido
 * (pago confirmado → conceder; reembolso digital → revocar). Operaciones idempotentes.
 */
public interface PurchaseAccessUseCase {

    /** Concede acceso al libro comprado (idempotente). */
    void grantAccess(String userId, String bookId);

    /** Revoca el acceso al libro (idempotente). */
    void revokeAccess(String userId, String bookId);
}

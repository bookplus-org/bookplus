package com.bookplus.order.domain.model;

import java.time.Instant;

/**
 * Prueba de entrega física: foto del paquete + firma del receptor (vista de dominio).
 * Los bytes viajan como datos opacos; el mapeo a columnas BYTEA vive en el adaptador.
 */
public record DeliveryProof(
        String  orderId,
        byte[]  photo,
        String  photoContentType,
        byte[]  signature,
        String  signatureContentType,
        String  receivedBy,
        String  deliveredBy,
        Instant createdAt
) {
    public boolean hasPhoto()     { return photo != null && photo.length > 0; }
    public boolean hasSignature() { return signature != null && signature.length > 0; }
}

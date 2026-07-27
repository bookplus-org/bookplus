package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.DeliveryProof;
import com.bookplus.order.domain.model.Order;

import java.util.Optional;

/**
 * Caso de uso de prueba de entrega: valida el código, marca el pedido como entregado y
 * persiste la prueba (foto + firma). El controlador solo se ocupa del HTTP/multipart.
 */
public interface DeliveryProofUseCase {

    /** Marca el pedido entregado con el código y guarda la prueba. Devuelve el pedido actualizado. */
    Order deliverWithProof(DeliverProofCommand command);

    /** Recupera la prueba de un pedido (foto/firma), si existe. */
    Optional<DeliveryProof> getProof(String orderId);

    record DeliverProofCommand(
            String orderId,
            String deliveryCode,
            String receivedBy,
            String deliveredBy,
            byte[] photo,
            String photoContentType,
            byte[] signature,
            String signatureContentType
    ) {}
}

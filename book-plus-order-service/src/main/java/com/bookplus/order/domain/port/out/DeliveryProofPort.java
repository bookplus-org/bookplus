package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.DeliveryProof;

import java.util.Optional;

/** Puerto de salida para la prueba de entrega: aísla la aplicación de JPA/BYTEA. */
public interface DeliveryProofPort {

    void save(DeliveryProof proof);

    Optional<DeliveryProof> findByOrderId(String orderId);
}

package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.model.DeliveryProof;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.in.DeliveryProofUseCase;
import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import com.bookplus.order.domain.port.out.DeliveryProofPort;
import com.bookplus.order.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

/**
 * Orquesta la entrega con prueba: reutiliza {@link UpdateOrderStatusUseCase#deliver} para validar
 * el código y marcar entregado, y persiste la prueba a través de {@link DeliveryProofPort}.
 * No conoce JPA ni multipart.
 */
@UseCase
@RequiredArgsConstructor
public class DeliveryProofUseCaseImpl implements DeliveryProofUseCase {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final DeliveryProofPort        deliveryProofPort;

    @Override
    public Order deliverWithProof(DeliverProofCommand cmd) {
        // Valida el código y marca el pedido entregado (lanza si el código es incorrecto).
        Order order = updateOrderStatusUseCase.deliver(cmd.orderId(), cmd.deliveryCode(), cmd.receivedBy());

        deliveryProofPort.save(new DeliveryProof(
                order.getId().value().toString(),
                cmd.photo(), cmd.photoContentType(),
                cmd.signature(), cmd.signatureContentType(),
                cmd.receivedBy(), cmd.deliveredBy(),
                Instant.now()));

        return order;
    }

    @Override
    public Optional<DeliveryProof> getProof(String orderId) {
        return deliveryProofPort.findByOrderId(orderId);
    }
}

package com.bookplus.order.adapter.out.persistence;

import com.bookplus.order.adapter.out.persistence.entity.DeliveryProofEntity;
import com.bookplus.order.adapter.out.persistence.repository.DeliveryProofJpaRepository;
import com.bookplus.order.domain.model.DeliveryProof;
import com.bookplus.order.domain.port.out.DeliveryProofPort;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Implementa {@link DeliveryProofPort} sobre JPA, mapeando entre {@link DeliveryProof} y la entidad. */
@PersistenceAdapter
@RequiredArgsConstructor
public class DeliveryProofPersistenceAdapter implements DeliveryProofPort {

    private final DeliveryProofJpaRepository repository;

    @Override
    public void save(DeliveryProof p) {
        repository.save(DeliveryProofEntity.builder()
                .orderId(UUID.fromString(p.orderId()))
                .photo(p.photo())
                .photoContentType(p.photoContentType())
                .signature(p.signature())
                .signatureContentType(p.signatureContentType())
                .receivedBy(p.receivedBy())
                .deliveredBy(p.deliveredBy())
                .createdAt(p.createdAt() != null ? p.createdAt() : Instant.now())
                .build());
    }

    @Override
    public Optional<DeliveryProof> findByOrderId(String orderId) {
        return repository.findById(UUID.fromString(orderId)).map(DeliveryProofPersistenceAdapter::toDomain);
    }

    private static DeliveryProof toDomain(DeliveryProofEntity e) {
        return new DeliveryProof(
                e.getOrderId().toString(),
                e.getPhoto(), e.getPhotoContentType(),
                e.getSignature(), e.getSignatureContentType(),
                e.getReceivedBy(), e.getDeliveredBy(),
                e.getCreatedAt());
    }
}

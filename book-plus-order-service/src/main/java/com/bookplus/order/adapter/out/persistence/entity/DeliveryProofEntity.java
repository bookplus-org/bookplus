package com.bookplus.order.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Prueba de entrega física: foto del paquete entregado + firma del receptor. */
@Entity
@Table(name = "delivery_proofs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryProofEntity {

    @Id
    @Column(name = "order_id", columnDefinition = "uuid")
    private UUID orderId;

    // Sin @Lob a propósito (mapeo BYTEA, no OID).
    @Column(name = "photo", columnDefinition = "bytea")
    private byte[] photo;

    @Column(name = "photo_content_type", length = 60)
    private String photoContentType;

    @Column(name = "signature", columnDefinition = "bytea")
    private byte[] signature;

    @Column(name = "signature_content_type", length = 60)
    private String signatureContentType;

    @Column(name = "received_by", length = 120)
    private String receivedBy;

    @Column(name = "delivered_by", length = 120)
    private String deliveredBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

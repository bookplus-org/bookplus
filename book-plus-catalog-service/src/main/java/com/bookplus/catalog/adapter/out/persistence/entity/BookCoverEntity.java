package com.bookplus.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Imagen de portada de un libro (subida por el admin). Aislada del agregado Book. */
@Entity
@Table(name = "book_covers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookCoverEntity {

    @Id
    @Column(name = "book_id", columnDefinition = "uuid")
    private UUID bookId;

    // Sin @Lob a propósito (mapeo BYTEA, no OID).
    @Column(name = "image", nullable = false, columnDefinition = "bytea")
    private byte[] image;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

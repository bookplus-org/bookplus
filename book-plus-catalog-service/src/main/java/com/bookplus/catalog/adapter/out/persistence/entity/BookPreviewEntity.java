package com.bookplus.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Muestra en PDF de un libro (primeras N páginas). Aislada del agregado Book
 * a propósito: el blob binario no debe contaminar el dominio del catálogo.
 */
@Entity
@Table(name = "book_previews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookPreviewEntity {

    @Id
    @Column(name = "book_id", columnDefinition = "uuid")
    private UUID bookId;

    // NB: sin @Lob a propósito — en PostgreSQL @Lob byte[] mapea a OID; aquí
    // queremos BYTEA, que es el mapeo por defecto de byte[].
    @Column(name = "preview_pdf", nullable = false, columnDefinition = "bytea")
    private byte[] previewPdf;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "source_pages")
    private Integer sourcePages;

    // PDF completo (solo-admin). Puede ser null si se subió antes de V7.
    @Column(name = "full_pdf", columnDefinition = "bytea")
    private byte[] fullPdf;

    @Column(name = "full_pages")
    private Integer fullPages;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

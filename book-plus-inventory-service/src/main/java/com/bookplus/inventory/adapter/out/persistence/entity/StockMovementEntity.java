package com.bookplus.inventory.adapter.out.persistence.entity;

import com.bookplus.inventory.domain.model.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "stock_movements",
    indexes = {
        @Index(name = "idx_movements_book_id",     columnList = "book_id"),
        @Index(name = "idx_movements_occurred_at", columnList = "occurred_at DESC"),
        @Index(name = "idx_movements_type",        columnList = "type"),
        @Index(name = "idx_movements_reference",   columnList = "reference_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovementEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private UUID bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "stock_before", nullable = false)
    private int stockBefore;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}

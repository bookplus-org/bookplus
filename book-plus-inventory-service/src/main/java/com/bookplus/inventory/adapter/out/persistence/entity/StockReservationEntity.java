package com.bookplus.inventory.adapter.out.persistence.entity;

import com.bookplus.inventory.domain.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "stock_reservations",
    indexes = {
        @Index(name = "idx_reservations_book_id",          columnList = "book_id"),
        @Index(name = "idx_reservations_order_id",         columnList = "order_id"),
        @Index(name = "idx_reservations_status",           columnList = "status"),
        @Index(name = "idx_reservations_expires_at",       columnList = "expires_at"),
        @Index(name = "idx_reservations_order_book",       columnList = "order_id, book_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockReservationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false, updatable = false)
    private UUID bookId;

    @Column(name = "order_id", nullable = false, updatable = false, length = 100)
    private String orderId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}

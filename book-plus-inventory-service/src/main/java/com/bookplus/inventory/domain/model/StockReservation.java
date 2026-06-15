package com.bookplus.inventory.domain.model;

import com.bookplus.inventory.domain.exception.DomainException;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity — Reserva de stock para un pedido pendiente.
 *
 * Una reserva bloquea stock durante el proceso de checkout para
 * evitar que dos pedidos simultáneos compren el mismo stock.
 * TTL por defecto: 30 minutos. Si no se confirma en ese tiempo, expira.
 */
public class StockReservation {

    private final ReservationId     id;
    private final BookId            bookId;
    private final String            orderId;
    private final String            userId;
    private final int               quantity;
    private       ReservationStatus status;
    private final Instant           createdAt;
    private final Instant           expiresAt;
    private       Instant           resolvedAt; // nullable — cuando se confirma o cancela

    private StockReservation(ReservationId id, BookId bookId, String orderId,
                              String userId, int quantity, ReservationStatus status,
                              Instant createdAt, Instant expiresAt, Instant resolvedAt) {
        this.id         = Objects.requireNonNull(id);
        this.bookId     = Objects.requireNonNull(bookId);
        this.orderId    = Objects.requireNonNull(orderId);
        this.userId     = Objects.requireNonNull(userId);
        this.quantity   = validateQuantity(quantity);
        this.status     = Objects.requireNonNull(status);
        this.createdAt  = Objects.requireNonNull(createdAt);
        this.expiresAt  = Objects.requireNonNull(expiresAt);
        this.resolvedAt = resolvedAt;
    }

    /** Crea una nueva reserva PENDING con TTL de 30 minutos. */
    public static StockReservation create(BookId bookId, String orderId,
                                          String userId, int quantity) {
        Instant now = Instant.now();
        return new StockReservation(
                ReservationId.generate(), bookId, orderId, userId, quantity,
                ReservationStatus.PENDING,
                now,
                now.plusSeconds(30L * 60),  // TTL: 30 min
                null
        );
    }

    public static StockReservation reconstitute(ReservationId id, BookId bookId,
                                                String orderId, String userId, int quantity,
                                                ReservationStatus status, Instant createdAt,
                                                Instant expiresAt, Instant resolvedAt) {
        return new StockReservation(id, bookId, orderId, userId, quantity,
                status, createdAt, expiresAt, resolvedAt);
    }

    // ── Comportamientos de dominio ────────────────────────────────────────

    /** Confirma la reserva — pedido pagado, stock consumido definitivamente. */
    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new DomainException("Cannot confirm reservation in status: " + status);
        }
        this.status     = ReservationStatus.CONFIRMED;
        this.resolvedAt = Instant.now();
    }

    /** Cancela la reserva — stock liberado. */
    public void cancel() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new DomainException("Cannot cancel a confirmed reservation");
        }
        if (status == ReservationStatus.CANCELLED) {
            throw new DomainException("Reservation is already cancelled");
        }
        this.status     = ReservationStatus.CANCELLED;
        this.resolvedAt = Instant.now();
    }

    /** Marca la reserva como expirada por TTL. */
    public void expire() {
        if (status != ReservationStatus.PENDING) {
            throw new DomainException("Cannot expire reservation in status: " + status);
        }
        this.status     = ReservationStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }

    public boolean isPending()   { return status == ReservationStatus.PENDING; }
    public boolean isExpired()   { return status == ReservationStatus.EXPIRED
                                       || (isPending() && Instant.now().isAfter(expiresAt)); }
    public boolean isActive()    { return isPending() && !isExpired(); }

    private static int validateQuantity(int qty) {
        if (qty <= 0) throw new DomainException("Reservation quantity must be positive, got: " + qty);
        return qty;
    }

    public ReservationId     getId()         { return id; }
    public BookId            getBookId()     { return bookId; }
    public String            getOrderId()    { return orderId; }
    public String            getUserId()     { return userId; }
    public int               getQuantity()   { return quantity; }
    public ReservationStatus getStatus()     { return status; }
    public Instant           getCreatedAt()  { return createdAt; }
    public Instant           getExpiresAt()  { return expiresAt; }
    public Instant           getResolvedAt() { return resolvedAt; }
}

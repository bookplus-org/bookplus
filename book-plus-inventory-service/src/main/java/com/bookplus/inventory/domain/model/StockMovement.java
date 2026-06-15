package com.bookplus.inventory.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity — Movimiento de inventario.
 * Registro inmutable de cada cambio en el stock de un libro.
 * Sirve como audit trail completo.
 */
public class StockMovement {

    private final MovementId   id;
    private final BookId       bookId;
    private final MovementType type;
    private final int          quantity;    // siempre positivo
    private final int          stockBefore;
    private final int          stockAfter;
    private final String       referenceId; // orderId, reservationId, etc. (nullable)
    private final String       notes;
    private final Instant      occurredAt;

    private StockMovement(MovementId id, BookId bookId, MovementType type,
                          int quantity, int stockBefore, int stockAfter,
                          String referenceId, String notes, Instant occurredAt) {
        this.id          = Objects.requireNonNull(id);
        this.bookId      = Objects.requireNonNull(bookId);
        this.type        = Objects.requireNonNull(type);
        this.quantity    = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter  = stockAfter;
        this.referenceId = referenceId;
        this.notes       = notes;
        this.occurredAt  = Objects.requireNonNull(occurredAt);
    }

    public static StockMovement record(BookId bookId, MovementType type,
                                       int quantity, int stockBefore, int stockAfter,
                                       String referenceId, String notes) {
        if (quantity <= 0) {
            throw new com.bookplus.inventory.domain.exception.DomainException(
                    "Movement quantity must be positive, got: " + quantity);
        }
        return new StockMovement(MovementId.generate(), bookId, type,
                quantity, stockBefore, stockAfter, referenceId, notes, Instant.now());
    }

    public static StockMovement reconstitute(MovementId id, BookId bookId, MovementType type,
                                             int quantity, int stockBefore, int stockAfter,
                                             String referenceId, String notes, Instant occurredAt) {
        return new StockMovement(id, bookId, type, quantity, stockBefore, stockAfter,
                referenceId, notes, occurredAt);
    }

    public MovementId   getId()          { return id; }
    public BookId       getBookId()      { return bookId; }
    public MovementType getType()        { return type; }
    public int          getQuantity()    { return quantity; }
    public int          getStockBefore() { return stockBefore; }
    public int          getStockAfter()  { return stockAfter; }
    public String       getReferenceId() { return referenceId; }
    public String       getNotes()       { return notes; }
    public Instant      getOccurredAt()  { return occurredAt; }
}

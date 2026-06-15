package com.bookplus.inventory.domain.model;

/**
 * Tipo de movimiento de inventario.
 *
 * IN  → entrada de stock (reposición, devolución)
 * OUT → salida de stock (venta confirmada)
 * RESERVED   → stock bloqueado para un pedido pendiente
 * UNRESERVED → reserva cancelada (pedido cancelado o expirado)
 * ADJUSTMENT → ajuste manual por inventario físico
 */
public enum MovementType {
    IN, OUT, RESERVED, UNRESERVED, ADJUSTMENT
}

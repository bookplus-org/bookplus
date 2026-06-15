package com.bookplus.inventory.domain.model;

/** Estado del ciclo de vida de una reserva de stock. */
public enum ReservationStatus {
    PENDING,    // reserva creada, stock bloqueado
    CONFIRMED,  // pedido pagado → stock consumido definitivamente
    CANCELLED,  // pedido cancelado → stock liberado
    EXPIRED     // TTL expirado sin confirmar → stock liberado
}

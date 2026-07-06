package com.bookplus.order.domain.audit;

/**
 * Entrada de un registro de auditoría a prueba de manipulación (hash chain).
 *
 * Cada entrada incluye el hash de la ANTERIOR ({@code prevHash}) dentro de su
 * propio hash, formando una cadena: cambiar cualquier entrada del pasado altera
 * su hash y rompe el enlace de todas las siguientes, de modo que la manipulación
 * es detectable (misma idea que una blockchain, sin la parte distribuida).
 */
public record AuditEntry(
        long   seq,
        long   timestampMillis,
        String actor,
        String action,
        String details,
        String prevHash,
        String hash) {
}

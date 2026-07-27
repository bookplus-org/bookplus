package com.bookplus.order.domain.port.out;

import java.time.Instant;

/**
 * Registro de eventos ya procesados (idempotencia de consumidores Kafka).
 * Aísla el guard de idempotencia de los detalles de JPA.
 */
public interface ProcessedEventPort {

    /**
     * Marca el evento como procesado si es la primera vez. Debe ejecutarse dentro de la
     * transacción del consumidor (participa en su commit/rollback).
     *
     * @return true si se marcó ahora (procesar), false si ya existía (duplicado → omitir)
     */
    boolean markIfNew(String eventId, String topic);

    /** Elimina registros anteriores al corte. Devuelve cuántos se borraron. */
    int deleteOlderThan(Instant cutoff);
}

package com.bookplus.order.domain.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén de eventos (event store) append-only: guarda la secuencia inmutable de
 * eventos por agregado y permite recuperarla para reconstruir el estado.
 *
 * La interfaz es un PUERTO de dominio; en producción su adaptador escribiría en
 * una tabla append-only (o Kafka / EventStoreDB). Aquí se incluye una
 * implementación en memoria, suficiente para las pruebas y para demostrar el
 * patrón sin acoplarse a infraestructura.
 */
public interface EventStore {

    /** Añade eventos al final del flujo del agregado (nunca se modifican los previos). */
    void append(String aggregateId, List<OrderEvent> events);

    /** Devuelve el historial completo del agregado, en orden. */
    List<OrderEvent> load(String aggregateId);

    /** Implementación en memoria (thread-safe) para pruebas y demostración. */
    final class InMemory implements EventStore {

        private final Map<String, List<OrderEvent>> streams = new ConcurrentHashMap<>();

        @Override
        public void append(String aggregateId, List<OrderEvent> events) {
            streams.computeIfAbsent(aggregateId, k -> new ArrayList<>()).addAll(events);
        }

        @Override
        public List<OrderEvent> load(String aggregateId) {
            return List.copyOf(streams.getOrDefault(aggregateId, List.of()));
        }
    }
}

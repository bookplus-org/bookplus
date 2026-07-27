package com.bookplus.order.adapter.out.persistence;

import com.bookplus.order.adapter.out.persistence.entity.ProcessedEventEntity;
import com.bookplus.order.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import com.bookplus.order.domain.port.out.ProcessedEventPort;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementa {@link ProcessedEventPort} sobre JPA (tabla processed_events).
 * markIfNew participa en la transacción del consumidor (MANDATORY): la marca se confirma
 * atómicamente con los cambios de negocio; si el consumidor falla, se revierte y Kafka reentrega.
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventPersistenceAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository repository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markIfNew(String eventId, String topic) {
        if (repository.existsByEventIdAndTopic(eventId, topic)) {
            return false;
        }
        try {
            repository.save(ProcessedEventEntity.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .build());
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Carrera: otra instancia insertó la misma fila de forma concurrente.
            log.info("Idempotency race skip: eventId={} topic={}", eventId, topic);
            return false;
        }
    }

    @Override
    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return repository.deleteOlderThan(cutoff);
    }
}

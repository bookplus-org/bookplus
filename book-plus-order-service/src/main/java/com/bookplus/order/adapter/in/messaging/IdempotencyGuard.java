package com.bookplus.order.adapter.in.messaging;

import com.bookplus.order.adapter.out.persistence.entity.ProcessedEventEntity;
import com.bookplus.order.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Idempotency guard for Kafka consumers.
 *
 * Usage pattern in a consumer:
 * <pre>
 *   if (!guard.tryAcquire(key, topic)) return; // already processed
 *   // ... process the event
 * </pre>
 *
 * tryAcquire() participates in the CALLER's transaction (Propagation.MANDATORY):
 * the idempotency record is committed atomically together with the business
 * changes. If the consumer fails after acquiring, the mark is rolled back too,
 * so Kafka redelivery re-processes the event instead of silently skipping it.
 *
 * The consumer method MUST be {@code @Transactional} so a transaction exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyGuard {

    private final ProcessedEventJpaRepository repository;

    /**
     * Returns true if the event should be processed (first time seen).
     * Returns false if it was already processed (duplicate → skip).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryAcquire(String eventId, String topic) {
        if (repository.existsByEventIdAndTopic(eventId, topic)) {
            log.info("Idempotency skip: eventId={} topic={} already processed", eventId, topic);
            return false;
        }
        try {
            repository.save(ProcessedEventEntity.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .build());
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another instance inserted the same row concurrently
            log.info("Idempotency race skip: eventId={} topic={}", eventId, topic);
            return false;
        }
    }

    /** Clean up processed_events records older than 30 days — runs daily */
    @Scheduled(fixedDelay = 86_400_000)
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0)
            log.info("IdempotencyGuard cleanup: deleted {} records older than 30 days", deleted);
    }
}

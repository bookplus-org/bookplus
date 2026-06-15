package com.bookplus.order.adapter.out.persistence.repository;

import com.bookplus.order.adapter.out.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, Long> {
    boolean existsByEventIdAndTopic(String eventId, String topic);

    @Modifying
    @Query("DELETE FROM ProcessedEventEntity p WHERE p.processedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}

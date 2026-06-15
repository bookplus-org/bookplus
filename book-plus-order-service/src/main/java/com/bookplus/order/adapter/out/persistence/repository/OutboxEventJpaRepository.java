package com.bookplus.order.adapter.out.persistence.repository;

import com.bookplus.order.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /** Fetch the next batch of PENDING events, ordered by creation time */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    /** Clean up successfully published events older than a cutoff */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}

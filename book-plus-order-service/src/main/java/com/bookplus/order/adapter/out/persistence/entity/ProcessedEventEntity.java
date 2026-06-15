package com.bookplus.order.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_events",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_processed_events",
               columnNames = {"event_id", "topic"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
}

package com.bookplus.report.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_events",
       indexes = {
           @Index(name = "idx_order_events_order_id",    columnList = "order_id"),
           @Index(name = "idx_order_events_user_id",     columnList = "user_id"),
           @Index(name = "idx_order_events_occurred_on", columnList = "occurred_on DESC"),
           @Index(name = "idx_order_events_type",        columnList = "event_type")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id",   nullable = false)
    private String orderId;

    @Column(name = "user_id",    nullable = false)
    private String userId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "total",      nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "currency",   nullable = false, length = 3)
    private String currency;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;    // JSON array, parsed in adapter

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;
}

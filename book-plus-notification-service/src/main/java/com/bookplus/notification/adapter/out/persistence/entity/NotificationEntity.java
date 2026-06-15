package com.bookplus.notification.adapter.out.persistence.entity;

import com.bookplus.notification.domain.model.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications",
       indexes = {
           @Index(name = "idx_notifications_user_id",    columnList = "user_id"),
           @Index(name = "idx_notifications_type",       columnList = "type"),
           @Index(name = "idx_notifications_reference",  columnList = "reference_id"),
           @Index(name = "idx_notifications_created_at", columnList = "created_at DESC")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}

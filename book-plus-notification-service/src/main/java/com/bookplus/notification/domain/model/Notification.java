package com.bookplus.notification.domain.model;

import lombok.Getter;

import java.time.Instant;

/**
 * Notification entity — not a full Aggregate Root (no complex invariants).
 * Treated as an audit record of every notification sent.
 *
 * Lifecycle: PENDING → SENT | FAILED
 */
@Getter
public class Notification {

    private final NotificationId      id;
    private final String              userId;         // recipient user ID
    private final String              recipientEmail;
    private final NotificationType    type;
    private final NotificationChannel channel;
    private final String              subject;
    private final String              body;           // plain-text or HTML
    private       NotificationStatus  status;
    private       String              failureReason;
    private final String              referenceId;   // orderId / paymentId / etc.
    private final Instant             createdAt;
    private       Instant             sentAt;

    private Notification(NotificationId id, String userId, String recipientEmail,
                         NotificationType type, NotificationChannel channel,
                         String subject, String body, String referenceId) {
        this.id             = id;
        this.userId         = userId;
        this.recipientEmail = recipientEmail;
        this.type           = type;
        this.channel        = channel;
        this.subject        = subject;
        this.body           = body;
        this.referenceId    = referenceId;
        this.status         = NotificationStatus.PENDING;
        this.createdAt      = Instant.now();
    }

    public static Notification create(String userId, String recipientEmail,
                                      NotificationType type, NotificationChannel channel,
                                      String subject, String body, String referenceId) {
        return new Notification(NotificationId.generate(), userId, recipientEmail,
                type, channel, subject, body, referenceId);
    }

    public static Notification reconstitute(NotificationId id, String userId, String recipientEmail,
                                             NotificationType type, NotificationChannel channel,
                                             String subject, String body, String referenceId,
                                             NotificationStatus status, String failureReason,
                                             Instant createdAt, Instant sentAt) {
        Notification n = new Notification(id, userId, recipientEmail, type, channel, subject, body, referenceId);
        n.status        = status;
        n.failureReason = failureReason;
        n.sentAt        = sentAt;
        return n;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status        = NotificationStatus.FAILED;
        this.failureReason = reason;
    }
}

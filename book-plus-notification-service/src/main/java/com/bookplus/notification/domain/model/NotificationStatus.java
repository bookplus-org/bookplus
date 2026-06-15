package com.bookplus.notification.domain.model;

public enum NotificationStatus {
    PENDING,    // queued, not yet sent
    SENT,       // delivered successfully to mail server / SMS gateway
    FAILED      // delivery failed after retries
}

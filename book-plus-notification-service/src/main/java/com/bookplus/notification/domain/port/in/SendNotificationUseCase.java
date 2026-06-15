package com.bookplus.notification.domain.port.in;

import com.bookplus.notification.domain.model.*;

public interface SendNotificationUseCase {

    Notification send(SendNotificationCommand command);

    record SendNotificationCommand(
            String              userId,
            String              recipientEmail,
            NotificationType    type,
            NotificationChannel channel,
            String              subject,
            String              body,
            String              referenceId     // orderId, paymentId, etc.
    ) {}
}

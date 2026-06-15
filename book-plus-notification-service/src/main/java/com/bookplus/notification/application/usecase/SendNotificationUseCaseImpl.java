package com.bookplus.notification.application.usecase;

import com.bookplus.notification.domain.model.*;
import com.bookplus.notification.domain.port.in.SendNotificationUseCase;
import com.bookplus.notification.domain.port.out.*;
import com.bookplus.notification.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class SendNotificationUseCaseImpl implements SendNotificationUseCase {

    private final SaveNotificationPort saveNotificationPort;
    private final EmailDeliveryPort    emailDeliveryPort;

    @Override
    public Notification send(SendNotificationCommand cmd) {
        Notification notification = Notification.create(
                cmd.userId(),
                cmd.recipientEmail(),
                cmd.type(),
                cmd.channel(),
                cmd.subject(),
                cmd.body(),
                cmd.referenceId()
        );

        // Persist first so we have an audit trail even if delivery fails
        saveNotificationPort.save(notification);

        if (cmd.channel() == NotificationChannel.EMAIL) {
            try {
                emailDeliveryPort.send(cmd.recipientEmail(), cmd.subject(), cmd.body());
                notification.markSent();
                log.info("Email sent to {} type={} ref={}", cmd.recipientEmail(), cmd.type(), cmd.referenceId());
            } catch (Exception ex) {
                notification.markFailed(ex.getMessage());
                log.error("Email delivery failed for {} type={}: {}", cmd.recipientEmail(), cmd.type(), ex.getMessage());
            }
        }

        // Persist final status (SENT or FAILED)
        return saveNotificationPort.save(notification);
    }
}

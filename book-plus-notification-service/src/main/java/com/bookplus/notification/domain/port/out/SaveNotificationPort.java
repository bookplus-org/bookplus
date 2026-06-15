package com.bookplus.notification.domain.port.out;
import com.bookplus.notification.domain.model.Notification;
public interface SaveNotificationPort {
    Notification save(Notification notification);
}

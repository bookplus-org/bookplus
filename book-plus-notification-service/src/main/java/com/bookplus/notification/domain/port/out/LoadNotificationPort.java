package com.bookplus.notification.domain.port.out;
import com.bookplus.notification.domain.model.Notification;
import java.util.List;
public interface LoadNotificationPort {
    List<Notification> findByUserId(String userId, int page, int size);
    long               countByUserId(String userId);
    List<Notification> findAll(int page, int size);
    long               countAll();
}

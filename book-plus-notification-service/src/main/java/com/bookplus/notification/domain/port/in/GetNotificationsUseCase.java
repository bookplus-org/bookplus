package com.bookplus.notification.domain.port.in;

import com.bookplus.notification.domain.model.Notification;

import java.util.List;

public interface GetNotificationsUseCase {
    List<Notification> getByUserId(String userId, int page, int size);
    long               countByUserId(String userId);
    List<Notification> getAll(int page, int size);
    long               countAll();
}

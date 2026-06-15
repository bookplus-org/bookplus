package com.bookplus.notification.application.usecase;

import com.bookplus.notification.domain.model.Notification;
import com.bookplus.notification.domain.port.in.GetNotificationsUseCase;
import com.bookplus.notification.domain.port.out.LoadNotificationPort;
import com.bookplus.notification.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase @RequiredArgsConstructor
public class GetNotificationsUseCaseImpl implements GetNotificationsUseCase {

    private final LoadNotificationPort loadNotificationPort;

    @Override
    public List<Notification> getByUserId(String userId, int page, int size) {
        return loadNotificationPort.findByUserId(userId, page, size);
    }

    @Override
    public long countByUserId(String userId) {
        return loadNotificationPort.countByUserId(userId);
    }

    @Override
    public List<Notification> getAll(int page, int size) {
        return loadNotificationPort.findAll(page, size);
    }

    @Override
    public long countAll() {
        return loadNotificationPort.countAll();
    }
}

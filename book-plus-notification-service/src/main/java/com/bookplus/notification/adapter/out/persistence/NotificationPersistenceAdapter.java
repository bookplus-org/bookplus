package com.bookplus.notification.adapter.out.persistence;

import com.bookplus.notification.adapter.out.persistence.entity.NotificationEntity;
import com.bookplus.notification.adapter.out.persistence.repository.NotificationJpaRepository;
import com.bookplus.notification.domain.model.*;
import com.bookplus.notification.domain.port.out.*;
import com.bookplus.notification.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@PersistenceAdapter
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements SaveNotificationPort, LoadNotificationPort {

    private final NotificationJpaRepository repository;

    @Override
    public Notification save(Notification n) {
        return toDomain(repository.save(toEntity(n)));
    }

    @Override
    public List<Notification> findByUserId(String userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByUserId(String userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public List<Notification> findAll(int page, int size) {
        return repository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countAll() {
        return repository.count();
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private Notification toDomain(NotificationEntity e) {
        return Notification.reconstitute(
                NotificationId.of(e.getId()),
                e.getUserId(),
                e.getRecipientEmail(),
                e.getType(),
                e.getChannel(),
                e.getSubject(),
                e.getBody(),
                e.getReferenceId(),
                e.getStatus(),
                e.getFailureReason(),
                e.getCreatedAt(),
                e.getSentAt()
        );
    }

    private NotificationEntity toEntity(Notification n) {
        return NotificationEntity.builder()
                .id(n.getId().value())
                .userId(n.getUserId())
                .recipientEmail(n.getRecipientEmail())
                .type(n.getType())
                .channel(n.getChannel())
                .subject(n.getSubject())
                .body(n.getBody())
                .status(n.getStatus())
                .failureReason(n.getFailureReason())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}

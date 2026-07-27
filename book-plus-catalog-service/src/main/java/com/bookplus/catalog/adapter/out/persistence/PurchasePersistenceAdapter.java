package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.entity.UserPurchaseEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.UserPurchaseJpaRepository;
import com.bookplus.catalog.domain.model.Purchase;
import com.bookplus.catalog.domain.port.out.PurchasePort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implementa {@link PurchasePort} sobre JPA (tabla user_purchases, clave user_id + book_id). */
@PersistenceAdapter
@RequiredArgsConstructor
public class PurchasePersistenceAdapter implements PurchasePort {

    private final UserPurchaseJpaRepository repository;

    @Override
    public List<String> findActiveBookIds(String userId) {
        return repository.findByUserIdAndActiveTrueOrderByPurchasedAtDesc(userId).stream()
                .map(p -> p.getBookId().toString())
                .toList();
    }

    @Override
    public Optional<Purchase> find(String userId, String bookId) {
        return repository.findByUserIdAndBookId(userId, UUID.fromString(bookId))
                .map(PurchasePersistenceAdapter::toDomain);
    }

    @Override
    public boolean hasActiveAccess(String userId, String bookId) {
        return repository.existsByUserIdAndBookIdAndActiveTrue(userId, UUID.fromString(bookId));
    }

    @Override
    public void save(Purchase p) {
        repository.save(UserPurchaseEntity.builder()
                .userId(p.userId())
                .bookId(UUID.fromString(p.bookId()))
                .purchasedAt(p.purchasedAt() != null ? p.purchasedAt() : Instant.now())
                .active(p.active())
                .downloaded(p.downloaded())
                .readProgress(p.readProgress())
                .build());
    }

    private static Purchase toDomain(UserPurchaseEntity e) {
        return new Purchase(
                e.getUserId(), e.getBookId().toString(), e.getPurchasedAt(),
                e.isActive(), e.isDownloaded(), e.getReadProgress());
    }
}

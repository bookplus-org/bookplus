package com.bookplus.catalog.adapter.out.persistence;

import com.bookplus.catalog.adapter.out.persistence.entity.UserFavoriteEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.UserFavoriteJpaRepository;
import com.bookplus.catalog.domain.port.out.FavoritePort;
import com.bookplus.catalog.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Implementa {@link FavoritePort} sobre JPA (tabla user_favorites, clave user_id + book_id). */
@PersistenceAdapter
@RequiredArgsConstructor
public class FavoritePersistenceAdapter implements FavoritePort {

    private final UserFavoriteJpaRepository repository;

    @Override
    public List<String> findBookIdsByUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> f.getBookId().toString())
                .toList();
    }

    @Override
    public boolean exists(String userId, String bookId) {
        return repository.existsByUserIdAndBookId(userId, UUID.fromString(bookId));
    }

    @Override
    public void add(String userId, String bookId) {
        repository.save(UserFavoriteEntity.builder()
                .userId(userId)
                .bookId(UUID.fromString(bookId))
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public void remove(String userId, String bookId) {
        repository.deleteByUserIdAndBookId(userId, UUID.fromString(bookId));
    }
}

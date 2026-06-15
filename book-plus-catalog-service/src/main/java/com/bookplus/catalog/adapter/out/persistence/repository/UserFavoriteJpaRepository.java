package com.bookplus.catalog.adapter.out.persistence.repository;

import com.bookplus.catalog.adapter.out.persistence.entity.UserFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface UserFavoriteJpaRepository
        extends JpaRepository<UserFavoriteEntity, UserFavoriteEntity.PK> {

    List<UserFavoriteEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndBookId(String userId, UUID bookId);

    @Transactional
    void deleteByUserIdAndBookId(String userId, UUID bookId);
}

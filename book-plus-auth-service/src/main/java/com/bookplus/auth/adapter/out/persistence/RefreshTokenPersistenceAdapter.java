package com.bookplus.auth.adapter.out.persistence;

import com.bookplus.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.bookplus.auth.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.bookplus.auth.domain.model.RefreshToken;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.out.LoadRefreshTokenPort;
import com.bookplus.auth.domain.port.out.SaveRefreshTokenPort;
import com.bookplus.auth.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements LoadRefreshTokenPort, SaveRefreshTokenPort {

    private final RefreshTokenJpaRepository repository;

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void revokeAllByUserId(UserId userId) {
        repository.revokeAllByUserId(userId.value());
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = toEntity(token);
        return toDomain(repository.save(entity));
    }

    @Override
    public void update(RefreshToken token) {
        repository.findById(token.getId()).ifPresent(entity -> {
            entity.setRevoked(token.isRevoked());
            repository.save(entity);
        });
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return RefreshToken.reconstitute(
                e.getId(), UserId.of(e.getUserId()), e.getTokenHash(),
                e.getExpiresAt(), e.getIpAddress(), e.getUserAgent(),
                e.isRevoked(), e.getCreatedAt());
    }

    private RefreshTokenEntity toEntity(RefreshToken d) {
        return RefreshTokenEntity.builder()
                .id(d.getId())
                .userId(d.getUserId().value())
                .tokenHash(d.getTokenHash())
                .expiresAt(d.getExpiresAt())
                .ipAddress(d.getIpAddress())
                .userAgent(d.getUserAgent())
                .revoked(d.isRevoked())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

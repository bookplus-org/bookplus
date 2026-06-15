package com.bookplus.auth.adapter.out.persistence;

import com.bookplus.auth.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.bookplus.auth.adapter.out.persistence.repository.PasswordResetTokenJpaRepository;
import com.bookplus.auth.domain.model.PasswordResetToken;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.out.LoadPasswordResetTokenPort;
import com.bookplus.auth.domain.port.out.SavePasswordResetTokenPort;
import com.bookplus.auth.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class PasswordResetTokenPersistenceAdapter
        implements LoadPasswordResetTokenPort, SavePasswordResetTokenPort {

    private final PasswordResetTokenJpaRepository repository;

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return toDomain(repository.save(toEntity(token)));
    }

    @Override
    public void update(PasswordResetToken token) {
        repository.findById(token.getId()).ifPresent(entity -> {
            entity.setUsed(token.isUsed());
            repository.save(entity);
        });
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity e) {
        return PasswordResetToken.reconstitute(
                e.getId(), UserId.of(e.getUserId()), e.getTokenHash(),
                e.getExpiresAt(), e.isUsed(), e.getCreatedAt());
    }

    private PasswordResetTokenEntity toEntity(PasswordResetToken d) {
        return PasswordResetTokenEntity.builder()
                .id(d.getId())
                .userId(d.getUserId().value())
                .tokenHash(d.getTokenHash())
                .expiresAt(d.getExpiresAt())
                .used(d.isUsed())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

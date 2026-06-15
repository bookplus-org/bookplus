package com.bookplus.auth.adapter.out.persistence.mapper;

import com.bookplus.auth.adapter.out.persistence.entity.RoleEntity;
import com.bookplus.auth.adapter.out.persistence.entity.UserEntity;
import com.bookplus.auth.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper entre el dominio y las entidades JPA.
 * El dominio NO conoce las entidades JPA — este mapper es el puente.
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        Set<UserRole> roles = entity.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        return User.reconstitute(
                UserId.of(entity.getId()),
                entity.getUsername(),
                Email.of(entity.getEmail()),
                entity.getPasswordHash(),
                roles,
                entity.isEnabled(),
                entity.isEmailVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(User domain, Set<RoleEntity> roleEntities) {
        return UserEntity.builder()
                .id(domain.getId().value())
                .username(domain.getUsername())
                .email(domain.getEmail().value())
                .passwordHash(domain.getPasswordHash())
                .enabled(domain.isEnabled())
                .emailVerified(domain.isEmailVerified())
                .roles(roleEntities)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

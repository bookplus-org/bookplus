package com.bookplus.auth.adapter.out.persistence.repository;

import com.bookplus.auth.adapter.out.persistence.entity.RoleEntity;
import com.bookplus.auth.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(UserRole name);
}

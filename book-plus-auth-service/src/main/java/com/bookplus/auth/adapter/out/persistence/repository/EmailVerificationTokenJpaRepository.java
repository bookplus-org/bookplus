package com.bookplus.auth.adapter.out.persistence.repository;

import com.bookplus.auth.adapter.out.persistence.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenJpaRepository
        extends JpaRepository<EmailVerificationTokenEntity, String> {
}

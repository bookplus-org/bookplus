package com.bookplus.order.adapter.out.persistence.repository;

import com.bookplus.order.adapter.out.persistence.entity.DeliveryProofEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryProofJpaRepository extends JpaRepository<DeliveryProofEntity, UUID> {
}

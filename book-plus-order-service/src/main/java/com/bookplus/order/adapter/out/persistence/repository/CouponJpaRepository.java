package com.bookplus.order.adapter.out.persistence.repository;

import com.bookplus.order.adapter.out.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, String> {
}

package com.bookplus.order.adapter.out.persistence;

import com.bookplus.order.adapter.out.persistence.entity.CouponEntity;
import com.bookplus.order.adapter.out.persistence.repository.CouponJpaRepository;
import com.bookplus.order.domain.model.Coupon;
import com.bookplus.order.domain.port.out.CouponPort;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

/** Implementa {@link CouponPort} sobre JPA, mapeando entre {@link Coupon} y {@link CouponEntity}. */
@PersistenceAdapter
@RequiredArgsConstructor
public class CouponPersistenceAdapter implements CouponPort {

    private final CouponJpaRepository repository;

    @Override
    public Optional<Coupon> findByCode(String code) {
        return repository.findById(code).map(CouponPersistenceAdapter::toDomain);
    }

    @Override
    public void save(Coupon c) {
        repository.save(CouponEntity.builder()
                .code(c.code())
                .discountType(c.discountType())
                .discountValue(c.discountValue())
                .minAmount(c.minAmount())
                .active(c.active())
                .expiresAt(c.expiresAt())
                .createdAt(Instant.now())
                .build());
    }

    private static Coupon toDomain(CouponEntity e) {
        return new Coupon(e.getCode(), e.getDiscountType(), e.getDiscountValue(),
                e.getMinAmount(), e.isActive(), e.getExpiresAt());
    }
}

package com.bookplus.order.adapter.out.persistence.repository;

import com.bookplus.order.adapter.out.persistence.entity.OrderEntity;
import com.bookplus.order.domain.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long              countByUserId(String userId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    long              countByStatus(OrderStatus status);

    List<OrderEntity> findByDeliveryTypeAndStatusInOrderByCreatedAtAsc(
            String deliveryType, Collection<OrderStatus> statuses);

    List<OrderEntity> findByStatus(OrderStatus status);
}

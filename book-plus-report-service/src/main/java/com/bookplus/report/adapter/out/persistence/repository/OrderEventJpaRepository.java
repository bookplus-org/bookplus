package com.bookplus.report.adapter.out.persistence.repository;

import com.bookplus.report.adapter.out.persistence.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventJpaRepository extends JpaRepository<OrderEventEntity, Long> {}

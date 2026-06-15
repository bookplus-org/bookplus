package com.bookplus.report.adapter.out.persistence.repository;

import com.bookplus.report.adapter.out.persistence.entity.DailySalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailySalesJpaRepository extends JpaRepository<DailySalesEntity, Long> {

    List<DailySalesEntity> findBySaleDateBetweenOrderBySaleDateAsc(
            LocalDate from, LocalDate to);

    @Query("""
           SELECT new com.bookplus.report.adapter.out.persistence.repository.TopBookProjection(
               oe.orderId, SUM(oe.total), COUNT(oe.id)
           )
           FROM OrderEventEntity oe
           WHERE CAST(oe.occurredOn AS LocalDate) BETWEEN :from AND :to
             AND oe.eventType = 'ORDER_CREATED'
           GROUP BY oe.orderId
           ORDER BY COUNT(oe.id) DESC
           LIMIT :limit
           """)
    List<TopBookProjection> findTopBooks(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit);
}

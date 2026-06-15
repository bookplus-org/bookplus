package com.bookplus.inventory.adapter.out.persistence.repository;

import com.bookplus.inventory.adapter.out.persistence.entity.StockReservationEntity;
import com.bookplus.inventory.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, UUID> {

    Optional<StockReservationEntity> findByOrderIdAndBookId(String orderId, UUID bookId);

    /** Reservas PENDING cuyo expiresAt es anterior al momento dado. */
    @Query("SELECT r FROM StockReservationEntity r " +
           "WHERE r.status = :status AND r.expiresAt < :before")
    List<StockReservationEntity> findByStatusAndExpiresAtBefore(
            ReservationStatus status, Instant before);
}

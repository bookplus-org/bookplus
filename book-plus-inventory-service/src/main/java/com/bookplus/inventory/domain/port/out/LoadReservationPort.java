package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.ReservationId;
import com.bookplus.inventory.domain.model.StockReservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadReservationPort {
    Optional<StockReservation> findById(ReservationId id);
    Optional<StockReservation> findByOrderIdAndBookId(String orderId, BookId bookId);
    List<StockReservation> findExpiredPending(Instant before);
}

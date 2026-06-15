package com.bookplus.inventory.adapter.out.persistence;

import com.bookplus.inventory.adapter.out.persistence.mapper.StockPersistenceMapper;
import com.bookplus.inventory.adapter.out.persistence.repository.*;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.out.*;
import com.bookplus.inventory.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class StockPersistenceAdapter
        implements LoadStockPort, SaveStockPort,
                   LoadReservationPort, SaveReservationPort,
                   SaveMovementPort, LoadMovementPort {

    private final StockJpaRepository            stockRepo;
    private final StockReservationJpaRepository reservationRepo;
    private final StockMovementJpaRepository    movementRepo;
    private final StockPersistenceMapper        mapper;

    // ── LoadStockPort ─────────────────────────────────────────────────────

    @Override
    public Optional<Stock> findByBookId(BookId bookId) {
        return stockRepo.findByBookId(bookId.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByBookId(BookId bookId) {
        return stockRepo.existsByBookId(bookId.value());
    }

    // ── SaveStockPort ─────────────────────────────────────────────────────

    @Override
    public Stock save(Stock stock) {
        return mapper.toDomain(stockRepo.save(mapper.toEntity(stock)));
    }

    // ── LoadReservationPort ───────────────────────────────────────────────

    @Override
    public Optional<StockReservation> findById(ReservationId id) {
        return reservationRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<StockReservation> findByOrderIdAndBookId(String orderId, BookId bookId) {
        return reservationRepo.findByOrderIdAndBookId(orderId, bookId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<StockReservation> findExpiredPending(Instant before) {
        return reservationRepo
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, before)
                .stream().map(mapper::toDomain).toList();
    }

    // ── SaveReservationPort ───────────────────────────────────────────────

    @Override
    public StockReservation save(StockReservation reservation) {
        return mapper.toDomain(reservationRepo.save(mapper.toEntity(reservation)));
    }

    // ── SaveMovementPort ──────────────────────────────────────────────────

    @Override
    public StockMovement save(StockMovement movement) {
        return mapper.toDomain(movementRepo.save(mapper.toEntity(movement)));
    }

    // ── LoadMovementPort ──────────────────────────────────────────────────

    @Override
    public Page<StockMovement> findByBookId(BookId bookId, Pageable pageable) {
        return movementRepo.findAllByBookId(bookId.value(), pageable)
                .map(mapper::toDomain);
    }
}

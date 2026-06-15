package com.bookplus.inventory.domain.port.out;

import com.bookplus.inventory.domain.model.StockReservation;

public interface SaveReservationPort {
    StockReservation save(StockReservation reservation);
}

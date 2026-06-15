package com.bookplus.inventory.domain.port.in;

public interface ReleaseReservationUseCase {
    void release(ReleaseReservationCommand command);

    record ReleaseReservationCommand(String reservationId, String orderId, String reason) {}
}

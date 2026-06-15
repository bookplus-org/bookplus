package com.bookplus.inventory.domain.port.in;

public interface ConfirmReservationUseCase {
    void confirm(ConfirmReservationCommand command);

    record ConfirmReservationCommand(String reservationId, String orderId) {}
}

package com.bookplus.order.domain.port.in;

import com.bookplus.order.domain.model.Order;

public interface CancelOrderUseCase {
    Order cancel(String orderId, String requestingUserId, String reason);

    /** Cancela sin comprobar propiedad (uso administrativo). */
    Order cancelAsAdmin(String orderId, String reason);
}

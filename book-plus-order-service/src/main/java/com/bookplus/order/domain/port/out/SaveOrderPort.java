package com.bookplus.order.domain.port.out;

import com.bookplus.order.domain.model.Order;

public interface SaveOrderPort {
    Order save(Order order);
}

package com.bookplus.report.domain.port.out;

import com.bookplus.report.domain.model.OrderEvent;

public interface SaveOrderEventPort {
    void save(OrderEvent event);
}

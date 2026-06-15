package com.bookplus.report.adapter.out.persistence.repository;

import java.math.BigDecimal;

public record TopBookProjection(
        String     orderId,
        BigDecimal totalRevenue,
        Long       orderCount
) {}

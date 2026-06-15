package com.bookplus.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Top-selling book snapshot for a given period */
@Getter
@Builder
public class TopBook {
    private final String     bookId;
    private final String     isbn;
    private final String     title;
    private final int        unitsSold;
    private final BigDecimal revenue;
}

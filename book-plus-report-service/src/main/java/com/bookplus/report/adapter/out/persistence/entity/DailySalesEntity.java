package com.bookplus.report.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_sales",
       uniqueConstraints = @UniqueConstraint(name = "uk_daily_sales_date", columnNames = "sale_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySalesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_date", nullable = false, unique = true)
    private LocalDate saleDate;

    @Column(name = "orders_count", nullable = false)
    private int ordersCount;

    @Column(name = "items_sold", nullable = false)
    private int itemsSold;

    @Column(name = "revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal revenue;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "cancellations", nullable = false)
    private int cancellations;

    @Column(name = "refunds", nullable = false)
    private int refunds;

    @Column(name = "refunded_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundedAmount;
}

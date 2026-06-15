package com.bookplus.report.adapter.out.persistence;

import com.bookplus.report.adapter.out.persistence.entity.*;
import com.bookplus.report.adapter.out.persistence.repository.*;
import com.bookplus.report.domain.model.*;
import com.bookplus.report.domain.port.out.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportPersistenceAdapter implements LoadMetricsPort, SaveOrderEventPort {

    private final DailySalesJpaRepository  dailySalesRepo;
    private final OrderEventJpaRepository  orderEventRepo;
    private final ObjectMapper             objectMapper;

    // ── LoadMetricsPort ───────────────────────────────────────────────────

    @Override
    public List<SalesMetric> findDailyMetrics(LocalDate from, LocalDate to) {
        return dailySalesRepo.findBySaleDateBetweenOrderBySaleDateAsc(from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<TopBook> findTopBooks(LocalDate from, LocalDate to, int limit) {
        return dailySalesRepo.findTopBooks(from, to, limit)
                .stream().map(p -> TopBook.builder()
                        .bookId(p.orderId())   // projection uses orderId as proxy
                        .isbn("N/A")
                        .title("Order " + p.orderId())
                        .unitsSold(p.orderCount().intValue())
                        .revenue(p.totalRevenue())
                        .build())
                .toList();
    }

    // ── SaveOrderEventPort ────────────────────────────────────────────────

    @Override
    public void save(OrderEvent event) {
        try {
            String itemsJson = objectMapper.writeValueAsString(event.getItems());
            OrderEventEntity entity = OrderEventEntity.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .eventType(event.getEventType())
                    .total(event.getTotal())
                    .currency(event.getCurrency())
                    .itemsJson(itemsJson)
                    .occurredOn(event.getOccurredOn())
                    .build();
            orderEventRepo.save(entity);

            // Upsert into daily_sales aggregation
            LocalDate day = event.getOccurredOn().atZone(ZoneOffset.UTC).toLocalDate();
            upsertDailySales(day, event);

        } catch (Exception ex) {
            log.error("Failed to persist order event orderId={}: {}", event.getOrderId(), ex.getMessage());
        }
    }

    // ── Internal aggregation ──────────────────────────────────────────────

    private void upsertDailySales(LocalDate day, OrderEvent event) {
        DailySalesEntity daily = dailySalesRepo
                .findBySaleDateBetweenOrderBySaleDateAsc(day, day)
                .stream().findFirst()
                .orElse(DailySalesEntity.builder()
                        .saleDate(day)
                        .ordersCount(0).itemsSold(0)
                        .revenue(BigDecimal.ZERO).currency(event.getCurrency())
                        .cancellations(0).refunds(0).refundedAmount(BigDecimal.ZERO)
                        .build());

        switch (event.getEventType()) {
            case "ORDER_CREATED" -> {
                daily.setOrdersCount(daily.getOrdersCount() + 1);
                int items = event.getItems() == null ? 0 :
                        event.getItems().stream().mapToInt(OrderEvent.ItemSnapshot::getQuantity).sum();
                daily.setItemsSold(daily.getItemsSold() + items);
                daily.setRevenue(daily.getRevenue().add(event.getTotal()));
            }
            case "ORDER_CANCELLED" -> daily.setCancellations(daily.getCancellations() + 1);
            case "PAYMENT_REFUNDED" -> {
                daily.setRefunds(daily.getRefunds() + 1);
                daily.setRefundedAmount(daily.getRefundedAmount().add(event.getTotal()));
            }
        }
        dailySalesRepo.save(daily);
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private SalesMetric toDomain(DailySalesEntity e) {
        return SalesMetric.builder()
                .date(e.getSaleDate())
                .ordersCount(e.getOrdersCount())
                .itemsSold(e.getItemsSold())
                .revenue(e.getRevenue())
                .currency(e.getCurrency())
                .cancellations(e.getCancellations())
                .refunds(e.getRefunds())
                .refundedAmount(e.getRefundedAmount())
                .build();
    }
}

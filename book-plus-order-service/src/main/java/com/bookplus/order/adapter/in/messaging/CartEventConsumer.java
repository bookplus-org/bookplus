package com.bookplus.order.adapter.in.messaging;

import com.bookplus.order.domain.model.*;
import com.bookplus.order.domain.port.in.CreateOrderUseCase;
import com.bookplus.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.bookplus.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand.ItemDto;
import com.bookplus.order.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Listens to cart.checked-out events and creates an Order.
 *
 * Default shipping address is built from the event payload.
 * In production this could be fetched from a user-service;
 * here we embed it in the event (cart-service includes it in CartCheckedOutEvent).
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class CartEventConsumer {

    private static final String TOPIC = "cart.checked-out";

    private final CreateOrderUseCase createOrderUseCase;
    private final IdempotencyGuard   idempotencyGuard;
    private final com.bookplus.order.application.coupon.CouponService couponService;

    @KafkaListener(topics = "cart.checked-out", groupId = "order-service-cart",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onCartCheckedOut(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.info("Received cart.checked-out event for cartId={}", key);

        // Idempotency guard — skip if already processed (Kafka at-least-once redelivery)
        if (!idempotencyGuard.tryAcquire(key, TOPIC)) return;

        try {
            String userId    = asString(payload.get("userId"));
            String userEmail = asString(payload.get("recipientEmail"));
            String cartId    = asString(payload.get("cartId"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) payload.get("items");

            List<ItemDto> items = rawItems.stream().map(i -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> price = (Map<String, Object>) i.get("unitPrice");
                return new ItemDto(
                        asString(i.get("bookId")),
                        (String) i.get("isbn"),
                        (String) i.get("title"),
                        (String) i.get("imageUrl"),
                        new BigDecimal(price.get("amount").toString()),
                        (String) price.getOrDefault("currency", "USD"),
                        ((Number) i.get("quantity")).intValue()
                );
            }).toList();

            @SuppressWarnings("unchecked")
            Map<String, Object> totalMap = (Map<String, Object>) payload.get("total");
            BigDecimal totalAmount   = new BigDecimal(totalMap.get("amount").toString());
            String     totalCurrency = (String) totalMap.getOrDefault("currency", "USD");

            // Default shipping address — in a real flow this would come from the event or user-service
            @SuppressWarnings("unchecked")
            Map<String, Object> addrMap = (Map<String, Object>) payload.getOrDefault("shippingAddress", Map.of(
                    "recipientName", "Customer",
                    "street", "TBD",
                    "city", "TBD",
                    "state", "",
                    "postalCode", "00000",
                    "country", "US"
            ));

            ShippingAddress address = new ShippingAddress(
                    (String) addrMap.get("recipientName"),
                    (String) addrMap.get("street"),
                    (String) addrMap.get("city"),
                    (String) addrMap.getOrDefault("state", ""),
                    (String) addrMap.get("postalCode"),
                    (String) addrMap.get("country")
            );

            // Payment method chosen at checkout (YAPE/PLIN/CARD/CASH); default CARD for safety
            String paymentMethod = asString(payload.getOrDefault("paymentMethod", "CARD"));
            String deliveryType  = asString(payload.getOrDefault("deliveryType", "PHYSICAL"));
            String couponCode    = asString(payload.get("couponCode"));

            // Aplicar cupón (si hay): el total del pedido es el importe ya descontado.
            var coupon = couponService.evaluate(couponCode, totalAmount);
            java.math.BigDecimal discount = coupon.valid() ? coupon.discount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal finalTotal = totalAmount.subtract(discount);
            String appliedCoupon = coupon.valid() ? coupon.code() : null;

            createOrderUseCase.createOrder(new CreateOrderCommand(
                    userId, userEmail, cartId, items, finalTotal, totalCurrency, address, paymentMethod, deliveryType,
                    appliedCoupon, discount
            ));

        } catch (Exception ex) {
            log.error("Failed to process cart.checked-out event cartId={}: {}", key, ex.getMessage(), ex);
            // Re-throw so Kafka retries (configured via retry/DLQ in KafkaConfig)
            throw new RuntimeException(ex);
        }
    }

    /**
     * Value objects (CartId, BookId) serialize as {@code {"value": "..."}} when the
     * producer uses the domain object directly. This flattens them to a plain string,
     * while leaving already-flat strings untouched.
     */
    private static String asString(Object o) {
        if (o instanceof Map<?, ?> m && m.containsKey("value")) {
            return String.valueOf(m.get("value"));
        }
        return o == null ? null : o.toString();
    }
}

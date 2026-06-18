package com.bookplus.order.contract;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.bookplus.order.adapter.in.messaging.CartEventConsumer;
import com.bookplus.order.adapter.in.messaging.IdempotencyGuard;
import com.bookplus.order.application.coupon.CouponService;
import com.bookplus.order.domain.port.in.CreateOrderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Contract test (Pact) — lado CONSUMIDOR.
 *
 * Define el contrato del evento "cart.checked-out" que order-service espera de
 * cart-service y verifica que el consumidor real lo procesa correctamente.
 * Al pasar, Pact genera el contrato en target/pacts, que luego verifica el productor.
 *
 * El contrato refleja la serialización REAL de cart: los value objects (CartId, BookId)
 * viajan como {"value": "<uuid>"} y Money como {amount, currency}.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "cart-service", providerType = ProviderType.ASYNCH)
@DisplayName("Contrato Pact: cart.checked-out (order es el consumidor)")
class CartCheckedOutContractTest {

    @Pact(consumer = "order-service")
    V4Pact cartCheckedOutPact(MessagePactBuilder builder) {
        DslPart body = newJsonBody(o -> {
            o.stringType("userId", "user-1");
            o.stringType("recipientEmail", "buyer@mail.com");
            o.object("cartId", c -> c.uuid("value"));
            o.stringType("paymentMethod", "CARD");
            o.stringType("deliveryType", "PHYSICAL");
            o.object("total", t -> {
                t.decimalType("amount", 59.98);
                t.stringType("currency", "USD");
            });
            o.object("shippingAddress", s -> {
                s.stringType("recipientName", "David");
                s.stringType("street", "Av. Siempre Viva 742");
                s.stringType("city", "Lima");
                s.stringType("state", "Lima");
                s.stringType("postalCode", "15001");
                s.stringType("country", "PE");
            });
            o.minArrayLike("items", 1, i -> {
                i.object("bookId", b -> b.uuid("value"));
                i.stringType("isbn", "9780132350884");
                i.stringType("title", "Clean Code");
                i.stringType("imageUrl", "img");
                i.integerType("quantity", 2);
                i.object("unitPrice", u -> {
                    u.decimalType("amount", 29.99);
                    u.stringType("currency", "USD");
                });
            });
        }).build();

        return builder
                .expectsToReceive("a cart checked-out event")
                .withContent(body)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "cartCheckedOutPact")
    @DisplayName("el consumidor de order procesa el evento y crea el pedido")
    void orderConsumesCartCheckedOut(List<V4Interaction.AsynchronousMessage> messages) throws Exception {
        byte[] bytes = messages.get(0).contentsAsBytes();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new ObjectMapper().readValue(bytes, Map.class);

        CreateOrderUseCase createOrder = mock(CreateOrderUseCase.class);
        IdempotencyGuard guard = mock(IdempotencyGuard.class);
        given(guard.tryAcquire(any(), any())).willReturn(true);
        CouponService coupons = mock(CouponService.class);
        given(coupons.evaluate(any(), any()))
                .willReturn(new CouponService.CouponResult(false, null, BigDecimal.ZERO, BigDecimal.ZERO, null));

        CartEventConsumer consumer = new CartEventConsumer(createOrder, guard, coupons);
        consumer.onCartCheckedOut(payload, "cart-1");

        then(createOrder).should().createOrder(any());
    }
}

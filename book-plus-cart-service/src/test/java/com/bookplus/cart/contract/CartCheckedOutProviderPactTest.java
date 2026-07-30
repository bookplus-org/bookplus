package com.bookplus.cart.contract;

import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.bookplus.cart.domain.event.CartCheckedOutEvent;
import com.bookplus.cart.domain.event.CartCheckedOutEvent.ShippingAddressDto;
import com.bookplus.cart.domain.model.BookId;
import com.bookplus.cart.domain.model.CartId;
import com.bookplus.cart.domain.model.CartItem;
import com.bookplus.cart.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Contract test (Pact) — lado PROVEEDOR (cart-service).
 *
 * Verifica que el evento REAL {@code cart.checked-out} que emite cart-service cumple el
 * contrato que generó el consumidor (order-service). El contrato se lee de la carpeta
 * target/pacts del módulo order (donde su test de consumidor lo publica).
 *
 * Flujo del contract testing (consumer-driven):
 *   1) order-service (consumidor) corre su test y genera el pact en su target/pacts.
 *   2) este test (proveedor) construye el evento real, lo serializa como en producción
 *      y Pact comprueba que encaja con los matchers del contrato.
 *
 * {@link IgnoreNoPactsToVerify}: si el pact del consumidor aún no se ha generado (p. ej. al
 * correr solo los tests de cart de forma aislada), el test se omite en vez de fallar.
 */
@Provider("cart-service")
@PactFolder("../book-plus-order-service/target/pacts")
@IgnoreNoPactsToVerify
@DisplayName("Contrato Pact: cart.checked-out (cart es el proveedor)")
class CartCheckedOutProviderPactTest {

    /** Mismo ObjectMapper que producción (Instant como ISO-8601, no como timestamp). */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // Verificación de mensajes asíncronos (no HTTP): el target es un MessageTestTarget.
        if (context != null) {
            context.setTarget(new MessageTestTarget());
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    /**
     * Produce el mensaje real. El nombre debe coincidir EXACTAMENTE con la descripción del
     * contrato del consumidor: builder.expectsToReceive("a cart checked-out event").
     */
    @PactVerifyProvider("a cart checked-out event")
    public String cartCheckedOutEvent() throws Exception {
        CartItem item = CartItem.create(
                BookId.of(UUID.randomUUID()),
                "Clean Code",
                "img",
                "9780132350884",
                2,
                Money.of(new BigDecimal("29.99"), "USD"));

        ShippingAddressDto address = new ShippingAddressDto(
                "David", "Av. Siempre Viva 742", "Lima", "Lima", "15001", "PE");

        CartCheckedOutEvent event = new CartCheckedOutEvent(
                CartId.of(UUID.randomUUID()),
                "user-1",
                "buyer@mail.com",
                List.of(item),
                Money.of(new BigDecimal("59.98"), "USD"),
                address,
                "CARD",
                "PHYSICAL",
                null);

        return mapper.writeValueAsString(event);
    }
}

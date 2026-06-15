package com.bookplus.order.application.usecase;

import com.bookplus.order.domain.model.*;
import com.bookplus.order.domain.model.Order;
import com.bookplus.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.bookplus.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand.ItemDto;
import com.bookplus.order.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCaseImpl — creación + outbox transaccional")
class CreateOrderUseCaseImplTest {

    @Mock private SaveOrderPort            saveOrderPort;
    @Mock private OutboxEventPublisherPort outboxPublisher;

    @InjectMocks
    private CreateOrderUseCaseImpl useCase;

    private ShippingAddress address() {
        return new ShippingAddress("John Doe", "123 St", "City", "ST", "12345", "USA");
    }

    private CreateOrderCommand command(String... bookIds) {
        List<ItemDto> items = java.util.Arrays.stream(bookIds)
                .map(id -> new ItemDto(id, "ISBN-" + id, "Book " + id, null,
                        new BigDecimal("15.00"), "USD", 1))
                .toList();
        return new CreateOrderCommand("user-1", "buyer@example.com", "cart-1", items,
                new BigDecimal("15.00").multiply(BigDecimal.valueOf(bookIds.length)),
                "USD", address(), "CARD", "PHYSICAL", null, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("createOrder() persiste el pedido y escribe los eventos en el outbox")
    void createOrder_success() {
        given(saveOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createOrder(command("book-1", "book-2"));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotal().amount()).isEqualByComparingTo("30.00");

        then(saveOrderPort).should().save(any());
        then(outboxPublisher).should().saveAll(eq("Order"), anyList());
    }

    @Test
    @DisplayName("createOrder() propaga la excepción si falla la escritura al outbox (rollback)")
    void createOrder_outboxFailure_rethrows() {
        given(saveOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("DB down")).given(outboxPublisher).saveAll(any(), anyList());

        assertThatThrownBy(() -> useCase.createOrder(command("book-1")))
                .isInstanceOf(RuntimeException.class);
    }
}

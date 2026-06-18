package com.bookplus.order.adapter.in.process;

import com.bookplus.order.domain.port.in.CancelOrderUseCase;
import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Delegates BPMN de la saga de compra")
class PurchaseSagaDelegatesTest {

    @Mock private UpdateOrderStatusUseCase updateStatusUseCase;
    @Mock private CancelOrderUseCase       cancelOrderUseCase;
    @Mock private DelegateExecution        execution;

    @Test
    @DisplayName("confirmPaymentDelegate confirma el pago del pedido de la variable orderId")
    void confirmPaymentDelegate_confirms() {
        given(execution.getVariable("orderId")).willReturn("ORD-1");
        ConfirmPaymentDelegate delegate = new ConfirmPaymentDelegate(updateStatusUseCase);

        delegate.execute(execution);

        then(updateStatusUseCase).should().confirmPayment("ORD-1");
    }

    @Test
    @DisplayName("cancelOrderDelegate cancela el pedido (compensación) con el motivo dado")
    void cancelOrderDelegate_cancels() {
        given(execution.getVariable("orderId")).willReturn("ORD-2");
        given(execution.getVariable("reason")).willReturn("Pago rechazado");
        CancelOrderDelegate delegate = new CancelOrderDelegate(cancelOrderUseCase);

        delegate.execute(execution);

        then(cancelOrderUseCase).should().cancelAsAdmin("ORD-2", "Pago rechazado");
    }

    @Test
    @DisplayName("cancelOrderDelegate usa un motivo por defecto si no hay variable reason")
    void cancelOrderDelegate_defaultReason() {
        given(execution.getVariable("orderId")).willReturn("ORD-3");
        given(execution.getVariable("reason")).willReturn(null);
        CancelOrderDelegate delegate = new CancelOrderDelegate(cancelOrderUseCase);

        delegate.execute(execution);

        then(cancelOrderUseCase).should().cancelAsAdmin("ORD-3", "Pago rechazado");
    }
}

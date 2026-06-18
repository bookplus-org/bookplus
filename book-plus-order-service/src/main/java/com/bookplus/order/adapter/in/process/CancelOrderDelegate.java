package com.bookplus.order.adapter.in.process;

import com.bookplus.order.domain.port.in.CancelOrderUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Service task del proceso BPMN "purchaseSaga": compensación. Si el pago falla,
 * cancela el pedido. Lee las variables {@code orderId} y {@code reason}.
 */
@Component("cancelOrderDelegate")
@RequiredArgsConstructor
@Slf4j
public class CancelOrderDelegate implements JavaDelegate {

    private final CancelOrderUseCase cancelOrderUseCase;

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = (String) execution.getVariable("orderId");
        Object reasonVar = execution.getVariable("reason");
        String reason = reasonVar != null ? reasonVar.toString() : "Pago rechazado";
        log.info("[BPMN] Compensación: cancelando pedido {} ({})", orderId, reason);
        cancelOrderUseCase.cancelAsAdmin(orderId, reason);
    }
}

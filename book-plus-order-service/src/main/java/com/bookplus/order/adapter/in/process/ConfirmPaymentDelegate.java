package com.bookplus.order.adapter.in.process;

import com.bookplus.order.domain.port.in.UpdateOrderStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Service task del proceso BPMN "purchaseSaga": confirma el pago del pedido.
 * Es un adaptador de entrada: traduce una tarea del motor de procesos (Camunda)
 * en una llamada al caso de uso de dominio, igual que haría un controlador REST.
 *
 * Lee la variable de proceso {@code orderId}.
 */
@Component("confirmPaymentDelegate")
@RequiredArgsConstructor
@Slf4j
public class ConfirmPaymentDelegate implements JavaDelegate {

    private final UpdateOrderStatusUseCase updateStatusUseCase;

    @Override
    public void execute(DelegateExecution execution) {
        String orderId = (String) execution.getVariable("orderId");
        log.info("[BPMN] Confirmando pago del pedido {}", orderId);
        updateStatusUseCase.confirmPayment(orderId);
    }
}

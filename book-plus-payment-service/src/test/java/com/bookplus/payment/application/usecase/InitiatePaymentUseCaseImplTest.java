package com.bookplus.payment.application.usecase;

import com.bookplus.payment.application.payment.PaymentMethodResolver;
import com.bookplus.payment.domain.exception.PaymentAlreadyExistsException;
import com.bookplus.payment.domain.model.*;
import com.bookplus.payment.domain.port.in.InitiatePaymentUseCase.InitiatePaymentCommand;
import com.bookplus.payment.domain.port.out.*;
import com.bookplus.payment.domain.service.PaymentAuthorization;
import com.bookplus.payment.domain.service.PaymentMethodHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitiatePaymentUseCaseImpl")
class InitiatePaymentUseCaseImplTest {

    @Mock private LoadPaymentPort          loadPaymentPort;
    @Mock private SavePaymentPort          savePaymentPort;
    @Mock private DomainEventPublisherPort eventPublisher;
    @Mock private PaymentMethodResolver    paymentMethodResolver;
    @Mock private PaymentMethodHandler     paymentMethodHandler;

    @InjectMocks
    private InitiatePaymentUseCaseImpl useCase;

    /** Pasarela simulada que aprueba el pago. */
    private void givenApprovedGateway() {
        given(paymentMethodResolver.resolve(any())).willReturn(paymentMethodHandler);
        given(paymentMethodHandler.authorize(any()))
                .willReturn(PaymentAuthorization.approved("TXN-TEST-001"));
    }

    private InitiatePaymentCommand command() {
        return new InitiatePaymentCommand(
                "order-001", "user-abc",
                new BigDecimal("59.99"), "USD",
                PaymentMethod.CREDIT_CARD
        );
    }

    @Test
    @DisplayName("initiate() creates payment, moves to PROCESSING, saves and publishes events")
    void initiate_success() {
        given(loadPaymentPort.findByOrderId("order-001")).willReturn(Optional.empty());
        given(savePaymentPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        givenApprovedGateway();

        Payment result = useCase.initiate(command());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getOrderId()).isEqualTo("order-001");
        assertThat(result.getAmount().amount()).isEqualByComparingTo("59.99");

        then(savePaymentPort).should().save(any());
        then(eventPublisher).should().publishAll(anyList());
    }

    @Test
    @DisplayName("initiate() throws PaymentAlreadyExistsException if payment already exists for order")
    void initiate_duplicateOrder_shouldThrow() {
        Payment existing = Payment.initiate("order-001", "user-abc",
                Money.of(new BigDecimal("59.99"), "USD"), PaymentMethod.CREDIT_CARD);
        given(loadPaymentPort.findByOrderId("order-001")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.initiate(command()))
                .isInstanceOf(PaymentAlreadyExistsException.class);

        then(savePaymentPort).should(never()).save(any());
    }

    @Test
    @DisplayName("initiate() rethrows when Kafka publish fails")
    void initiate_kafkaFailure_rethrows() {
        given(loadPaymentPort.findByOrderId(any())).willReturn(Optional.empty());
        given(savePaymentPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        givenApprovedGateway();
        willThrow(new RuntimeException("Kafka down")).given(eventPublisher).publishAll(any());

        assertThatThrownBy(() -> useCase.initiate(command()))
                .isInstanceOf(RuntimeException.class);
    }
}

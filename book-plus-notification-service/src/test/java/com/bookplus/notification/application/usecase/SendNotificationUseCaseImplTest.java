package com.bookplus.notification.application.usecase;

import com.bookplus.notification.domain.model.*;
import com.bookplus.notification.domain.port.in.SendNotificationUseCase.SendNotificationCommand;
import com.bookplus.notification.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendNotificationUseCaseImpl")
class SendNotificationUseCaseImplTest {

    @Mock private SaveNotificationPort saveNotificationPort;
    @Mock private EmailDeliveryPort    emailDeliveryPort;

    @InjectMocks
    private SendNotificationUseCaseImpl useCase;

    private SendNotificationCommand emailCommand() {
        return new SendNotificationCommand(
                "user-1", "user@example.com",
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL,
                "Order Received", "<p>Your order was created</p>", "order-001"
        );
    }

    @Test
    @DisplayName("send() delivers email, marks SENT, and persists twice")
    void send_emailSuccess() {
        given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(emailDeliveryPort).send(anyString(), anyString(), anyString());

        Notification result = useCase.send(emailCommand());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isNotNull();

        // saved twice: once as PENDING, once as SENT
        then(saveNotificationPort).should(times(2)).save(any());
        then(emailDeliveryPort).should().send("user@example.com", "Order Received", "<p>Your order was created</p>");
    }

    @Test
    @DisplayName("send() marks FAILED when email delivery throws, but does not rethrow")
    void send_emailDeliveryFails_marksFailedAndReturns() {
        given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("SMTP timeout")).given(emailDeliveryPort).send(anyString(), anyString(), anyString());

        Notification result = useCase.send(emailCommand());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getFailureReason()).contains("SMTP timeout");
        // still persisted twice (PENDING → FAILED)
        then(saveNotificationPort).should(times(2)).save(any());
    }

    @Test
    @DisplayName("send() does not invoke emailDeliveryPort for non-EMAIL channels")
    void send_nonEmailChannel_skipDelivery() {
        given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SendNotificationCommand smsCommand = new SendNotificationCommand(
                "user-1", "+1234567890",
                NotificationType.ORDER_SHIPPED, NotificationChannel.SMS,
                "Shipped", "Your order shipped", "order-002"
        );

        useCase.send(smsCommand);

        then(emailDeliveryPort).should(never()).send(anyString(), anyString(), anyString());
    }
}

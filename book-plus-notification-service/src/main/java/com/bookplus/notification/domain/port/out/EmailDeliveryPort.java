package com.bookplus.notification.domain.port.out;

/**
 * Output port for email delivery.
 * The implementation (JavaMailSender) lives in the infrastructure layer.
 * The domain only knows "send an email to this address with this subject/body".
 */
public interface EmailDeliveryPort {
    void send(String to, String subject, String htmlBody);
}

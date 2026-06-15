package com.bookplus.auth.domain.port.out;

/** Puerto de salida — envío de correos transaccionales. */
public interface EmailNotificationPort {
    void sendPasswordResetEmail(String toEmail, String username, String resetLink);
    void sendWelcomeEmail(String toEmail, String username);
    void sendVerificationEmail(String toEmail, String username, String verifyLink);
}

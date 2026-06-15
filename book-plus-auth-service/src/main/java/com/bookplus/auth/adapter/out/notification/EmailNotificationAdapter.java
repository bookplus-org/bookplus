package com.bookplus.auth.adapter.out.notification;

import com.bookplus.auth.domain.port.out.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de email — usa Spring Mail (SMTP / MailHog en local).
 *
 * En desarrollo se configura MailHog como servidor SMTP local.
 * En producción se reemplaza por SendGrid o Amazon SES cambiando
 * solo las propiedades de spring.mail.* en application.yml.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@bookplus.local}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Book+ — Restablecer contraseña");
            message.setText("""
                    Hola %s,

                    Recibimos una solicitud para restablecer la contraseña de tu cuenta Book+.

                    Haz clic en el siguiente enlace (válido por 1 hora):
                    %s

                    Si no solicitaste este cambio, ignora este correo.

                    Equipo Book+
                    """.formatted(username, resetLink));
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Bienvenido a Book+");
            message.setText("""
                    Hola %s,

                    ¡Bienvenido a Book+! Tu cuenta ha sido creada exitosamente.

                    Ya puedes explorar nuestro catálogo de libros y comenzar a comprar.

                    Equipo Book+
                    """.formatted(username));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendVerificationEmail(String toEmail, String username, String verifyLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Book+ — Verifica tu correo");
            message.setText("""
                    Hola %s,

                    Gracias por registrarte en Book+. Para activar tu cuenta, verifica tu correo
                    haciendo clic en el siguiente enlace (válido por 24 horas):

                    %s

                    Si no creaste esta cuenta, ignora este correo.

                    Equipo Book+
                    """.formatted(username, verifyLink));
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }
}

package com.bookplus.notification.adapter.out.email;

import com.bookplus.notification.domain.port.out.EmailDeliveryPort;
import com.bookplus.notification.shared.annotation.PersistenceAdapter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailAdapter implements EmailDeliveryPort {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from:noreply@bookplus.com}")
    private String fromAddress;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true = isHtml
            mailSender.send(message);
            log.debug("Email dispatched to {} subject='{}'", to, subject);
        } catch (MessagingException ex) {
            throw new RuntimeException("Failed to send email to " + to, ex);
        }
    }
}

package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.sender;

import com.clinic.c46.NotificationService.domain.exception.NotificationSendingException;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.EmailContentType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSenderImpl implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:no-reply@my-domain.com}")
    private String fromEmail;


    private MimeMessage createMessage(String to, String subject, String content,
            EmailContentType emailContentType) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, emailContentType == EmailContentType.HTML); // true = gửi HTML
        helper.setFrom(fromEmail);
        return message;
    }


    @Override
    @Retryable(retryFor = {NotificationSendingException.class}, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public void sendEmail(String to, String subject, String html, EmailContentType emailContentType) {
        try {
            MimeMessage message = createMessage(to, subject, html, emailContentType);
            sendMessage(message, to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new NotificationSendingException("Failed to send email", e);
        }
    }

    private void sendMessage(MimeMessage message, String to) {
        try {
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new NotificationSendingException("Failed to send email to " + to, e);
        }
    }

    @Recover
    public void recover(NotificationSendingException e, String to, String subject, String html, EmailContentType type) {
        log.error("email_recover_failure to={} subject={} type={} reason={}", to, subject, type, e.getMessage(), e);

    }
}

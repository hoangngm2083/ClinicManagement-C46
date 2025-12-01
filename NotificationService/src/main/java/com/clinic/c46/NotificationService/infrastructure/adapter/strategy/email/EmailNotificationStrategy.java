package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email;

import com.clinic.c46.NotificationService.application.service.notification.NotificationStrategy;
import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy xử lý việc gửi email
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationStrategy implements NotificationStrategy {

    private final EmailSender emailSender;

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void sendNotification(NotificationEvent event) {
        try {
            String subject = event.getSubject() != null ? event.getSubject() : "Thông báo từ Clinic C46";
            String content = event.getContent();

            emailSender.sendEmail(event.getRecipient(), subject, content, EmailContentType.HTML);

            log.info("Email notification sent successfully to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send email notification to: {}", event.getRecipient(), e);
            throw new RuntimeException("Email notification failed", e);
        }
    }

    @Override
    public String getStrategyName() {
        return "EmailNotificationStrategy";
    }
}

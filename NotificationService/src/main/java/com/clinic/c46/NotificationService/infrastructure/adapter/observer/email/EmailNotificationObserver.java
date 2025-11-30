package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.application.service.notification.NotificationObserver;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observer xử lý việc gửi email
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationObserver implements NotificationObserver {

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
    public String getObserverName() {
        return "EmailNotificationObserver";
    }
}

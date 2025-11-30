package com.clinic.c46.NotificationService.application.service.notification;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service chung để gửi thông báo qua nhiều kênh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSenderService {

    private final NotificationSubject notificationSubject;

    public void sendEmail(String userId, String recipient, String subject, String content) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .message("Email notification")
                .channel(NotificationChannel.EMAIL)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        notificationSubject.sendNotification(event);
    }

    public void sendSMS(String userId, String phoneNumber, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .message(message)
                .channel(NotificationChannel.SMS)
                .recipient(phoneNumber)
                .content(message)
                .timestamp(LocalDateTime.now())
                .build();

        notificationSubject.sendNotification(event);
    }

    public void sendZaloMessage(String userId, String zaloId, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .message(message)
                .channel(NotificationChannel.ZALO)
                .recipient(zaloId)
                .content(message)
                .timestamp(LocalDateTime.now())
                .build();

        notificationSubject.sendNotification(event);
    }

    /**
     * Gửi thông báo tùy chỉnh qua bất kỳ kênh nào
     */
    public void sendNotification(NotificationEvent event) {
        notificationSubject.sendNotification(event);
    }
}

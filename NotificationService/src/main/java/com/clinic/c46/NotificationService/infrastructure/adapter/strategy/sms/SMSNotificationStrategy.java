package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.sms;

import com.clinic.c46.NotificationService.application.service.notification.NotificationStrategy;
import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy xử lý việc gửi SMS
 */
@Component
@Slf4j
public class SMSNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void sendNotification(NotificationEvent event) {
        // Giả lập gửi SMS qua Twilio, Viettel, etc.
        log.info("Sending SMS to {}: {}", event.getRecipient(), event.getMessage());

        // TODO: Implement actual SMS sending
        // smsService.send(event.getRecipient(), event.getMessage());

        log.info("SMS notification sent successfully to: {}", event.getRecipient());
    }

    @Override
    public String getStrategyName() {
        return "SMSNotificationStrategy";
    }
}

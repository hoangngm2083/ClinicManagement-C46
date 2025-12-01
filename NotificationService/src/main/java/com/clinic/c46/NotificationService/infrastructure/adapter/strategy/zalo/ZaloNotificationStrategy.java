package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.zalo;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.application.service.notification.NotificationStrategy;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy xử lý việc gửi thông báo qua Zalo
 */
@Component
@Slf4j
public class ZaloNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.ZALO;
    }

    @Override
    public void sendNotification(NotificationEvent event) {
        // Giả lập gửi qua Zalo OA API
        log.info("Sending Zalo message to {}: {}", event.getRecipient(), event.getMessage());

        // TODO: Implement Zalo OA API integration
        // zaloService.sendMessage(event.getRecipient(), event.getMessage());

        log.info("Zalo notification sent successfully to: {}", event.getRecipient());
    }

    @Override
    public String getStrategyName() {
        return "ZaloNotificationStrategy";
    }
}

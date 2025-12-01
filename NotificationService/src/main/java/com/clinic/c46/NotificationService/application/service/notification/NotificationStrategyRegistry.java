package com.clinic.c46.NotificationService.application.service.notification;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry quản lý các notification strategies
 * Thay thế Observer Pattern bằng Strategy Pattern để đơn giản hóa
 */
@Component
@Slf4j
public class NotificationStrategyRegistry {

    // Map kênh -> strategy (mỗi kênh chỉ có 1 strategy)
    private final Map<NotificationChannel, NotificationStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * Đăng ký strategy cho một kênh cụ thể
     */
    public void registerStrategy(NotificationStrategy strategy) {
        NotificationChannel channel = strategy.getSupportedChannel();
        strategies.put(channel, strategy);
        log.info("Registered notification strategy: {} for channel: {}", strategy.getStrategyName(), channel);
    }

    /**
     * Gửi thông báo bằng strategy phù hợp với kênh
     */
    public void sendNotification(NotificationEvent event) {
        NotificationChannel channel = event.getChannel();
        NotificationStrategy strategy = strategies.get(channel);

        if (strategy == null) {
            log.error("No strategy registered for channel: {}", channel);
            throw new IllegalArgumentException("No strategy found for channel: " + channel);
        }

        log.info("Sending notification via strategy: {} for channel: {}", strategy.getStrategyName(), channel);

        try {
            strategy.sendNotification(event);
        } catch (Exception e) {
            log.error("Error in strategy {} when sending notification", strategy.getStrategyName(), e);
            throw e;
        }
    }

    /**
     * Lấy strategy cho một kênh
     */
    public NotificationStrategy getStrategy(NotificationChannel channel) {
        return strategies.get(channel);
    }

    /**
     * Lấy số lượng strategies đã đăng ký
     */
    public int getStrategyCount() {
        return strategies.size();
    }
}

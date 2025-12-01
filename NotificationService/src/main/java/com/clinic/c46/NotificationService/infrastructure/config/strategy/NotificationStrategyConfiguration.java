package com.clinic.c46.NotificationService.infrastructure.config.strategy;

import com.clinic.c46.NotificationService.application.service.notification.NotificationStrategyRegistry;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.EmailNotificationStrategy;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.sms.SMSNotificationStrategy;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.zalo.ZaloNotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configuration để tự động đăng ký các notification strategies khi ứng dụng khởi động
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationStrategyConfiguration {

    private final NotificationStrategyRegistry notificationStrategyRegistry;
    private final EmailNotificationStrategy emailNotificationStrategy;
    private final SMSNotificationStrategy smsNotificationStrategy;
    private final ZaloNotificationStrategy zaloNotificationStrategy;

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefreshed() {
        registerStrategies();
    }

    private void registerStrategies() {
        notificationStrategyRegistry.registerStrategy(emailNotificationStrategy);
        notificationStrategyRegistry.registerStrategy(smsNotificationStrategy);
        notificationStrategyRegistry.registerStrategy(zaloNotificationStrategy);

        log.info("Notification strategies registered successfully. Total strategies: {}",
                notificationStrategyRegistry.getStrategyCount());
    }
}

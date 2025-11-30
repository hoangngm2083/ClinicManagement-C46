package com.clinic.c46.NotificationService.infrastructure.config.observer;

import com.clinic.c46.NotificationService.application.service.notification.NotificationSubject;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.EmailNotificationObserver;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.sms.SMSNotificationObserver;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.zalo.ZaloNotificationObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configuration để tự động đăng ký các notification observers khi ứng dụng khởi động
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationObserverConfiguration {

    private final NotificationSubject notificationSubject;
    private final EmailNotificationObserver emailNotificationObserver;
    private final SMSNotificationObserver smsNotificationObserver;
    private final ZaloNotificationObserver zaloNotificationObserver;

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefreshed() {
        registerObservers();
    }

    private void registerObservers() {
        notificationSubject.registerObserver(emailNotificationObserver);
        notificationSubject.registerObserver(smsNotificationObserver);
        notificationSubject.registerObserver(zaloNotificationObserver);

        log.info("Notification observers registered successfully. Total observers: {}",
                notificationSubject.getObserverCount(NotificationChannel.EMAIL) + notificationSubject.getObserverCount(
                        NotificationChannel.SMS) + notificationSubject.getObserverCount(NotificationChannel.ZALO));
    }
}

package com.clinic.c46.NotificationService.application.service.notification;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;

/**
 * Strategy interface cho việc gửi thông báo qua các kênh khác nhau
 * Thay thế Observer Pattern bằng Strategy Pattern cho hiệu suất và đơn giản hơn
 */
public interface NotificationStrategy {

    /**
     * Kênh mà strategy này xử lý
     */
    NotificationChannel getSupportedChannel();

    /**
     * Xử lý việc gửi thông báo
     */
    void sendNotification(NotificationEvent event);

    /**
     * Tên của strategy để debug
     */
    String getStrategyName();
}

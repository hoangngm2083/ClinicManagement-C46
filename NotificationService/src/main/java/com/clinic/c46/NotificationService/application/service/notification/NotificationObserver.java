package com.clinic.c46.NotificationService.application.service.notification;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;

/**
 * Observer interface cho việc gửi thông báo qua các kênh khác nhau
 */
public interface NotificationObserver {

    /**
     * Kênh mà observer này xử lý
     */
    NotificationChannel getSupportedChannel();

    /**
     * Xử lý việc gửi thông báo
     */
    void sendNotification(NotificationEvent event);

    /**
     * Kiểm tra xem observer có thể xử lý event này không
     */
    default boolean canHandle(NotificationEvent event) {
        return getSupportedChannel() == event.getChannel();
    }

    /**
     * Tên của observer để debug
     */
    String getObserverName();
}

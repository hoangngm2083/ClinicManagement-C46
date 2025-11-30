package com.clinic.c46.NotificationService.application.service.notification;

import com.clinic.c46.NotificationService.domain.event.NotificationEvent;
import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject quản lý tất cả notification observers
 * Mỗi kênh có thể có nhiều observers
 */
@Component
@Slf4j
public class NotificationSubject {

    // Map kênh -> danh sách observers
    private final Map<NotificationChannel, List<NotificationObserver>> channelObservers = new ConcurrentHashMap<>(); // Hoạt động an toàn trong đa luồng, đọc/ghi cùng lúc không bị race condition.

    /**
     * Đăng ký observer cho một kênh cụ thể
     */
    public void registerObserver(NotificationObserver observer) {
        NotificationChannel channel = observer.getSupportedChannel();
        // Kiểm tra tồn tại + tạo mới + put value đều trong một thao tác atomically, thread-safe.
        channelObservers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()) // Workload = 95% đọc + 5% ghi
                .add(observer);
        log.info("Registered notification observer: {} for channel: {}", observer.getObserverName(), channel);
    }

    /**
     * Hủy đăng ký observer
     */
    public void removeObserver(NotificationObserver observer) {
        NotificationChannel channel = observer.getSupportedChannel();
        List<NotificationObserver> observers = channelObservers.get(channel);
        if (observers != null) {
            observers.remove(observer);
            log.info("Removed notification observer: {} from channel: {}", observer.getObserverName(), channel);
        }
    }

    /**
     * Gửi thông báo đến tất cả observers phù hợp với kênh
     */
    public void sendNotification(NotificationEvent event) {
        NotificationChannel channel = event.getChannel();
        List<NotificationObserver> observers = channelObservers.get(channel);

        if (observers == null || observers.isEmpty()) {
            log.warn("No observers registered for channel: {}", channel);
            return;
        }

        log.info("Sending notification via {} observers for channel: {}", observers.size(), channel);

        for (NotificationObserver observer : observers) {
            try {
                if (observer.canHandle(event)) {
                    observer.sendNotification(event);
                }
            } catch (Exception e) {
                log.error("Error in observer {} when sending notification", observer.getObserverName(), e);
            }
        }
    }

    /**
     * Lấy số lượng observers cho một kênh
     */
    public int getObserverCount(NotificationChannel channel) {
        List<NotificationObserver> observers = channelObservers.get(channel);
        return observers != null ? observers.size() : 0;
    }
}

package com.clinic.c46.NotificationService.domain.event;

import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sự kiện thông báo chung cho tất cả kênh
 */
@Data
@Builder
public class NotificationEvent {
    private String userId;
    private String message;
    private NotificationChannel channel;
    private String recipient; // email, phone number, zalo id, etc.
    private String subject; // optional for email
    private String content; // HTML content for email, text for SMS, etc.
    private LocalDateTime timestamp;

    // Template variables cho email
    private Object templateVariables; // Có thể là Map<String, Object> hoặc DTO cụ thể
}

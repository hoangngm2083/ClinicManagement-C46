package com.clinic.c46.NotificationService.domain.projection;


import com.clinic.c46.NotificationService.domain.valueObject.NotificationChannel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;

import java.time.Instant;

@Entity

public class NotificationProjection {
    @Id
    String id;
    String userId;
    String message;
    NotificationChannel channel; // SSE, EMAIL, ZALO
    Instant createdAt;
}
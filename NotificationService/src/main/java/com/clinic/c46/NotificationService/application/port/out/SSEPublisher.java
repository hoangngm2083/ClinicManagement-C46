package com.clinic.c46.NotificationService.application.port.out;

import com.clinic.c46.NotificationService.domain.projection.NotificationProjection;

public interface SSEPublisher {
    void publish(NotificationProjection notification);
}

package com.clinic.c46.NotificationService.application.port.out;

import com.clinic.c46.NotificationService.domain.projection.NotificationProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationProjection, String> {
}

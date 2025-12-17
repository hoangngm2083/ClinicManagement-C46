package com.clinic.c46.MedicalPackageService.domain.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BulkImportCreatedEvent(
        String bulkId,
        String entityType,
        String importFileUrl,
        String status,
        LocalDateTime createdAt
) {
}

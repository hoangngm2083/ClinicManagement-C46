package com.clinic.c46.MedicalPackageService.domain.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BulkImportStatusUpdatedEvent(
        String bulkId,
        String status,
        Integer totalRows,
        Integer successfulRows,
        Integer failedRows,
        String resultCsvUrl,
        LocalDateTime updatedAt
) {
}

package com.clinic.c46.CommonService.event.file;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CsvFileUploadedEvent(
        String fileId,
        String fileName,
        String fileUrl,
        LocalDateTime uploadedAt
) {
}

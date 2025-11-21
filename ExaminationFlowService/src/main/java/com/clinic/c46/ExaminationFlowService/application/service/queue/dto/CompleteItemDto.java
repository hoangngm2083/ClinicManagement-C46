package com.clinic.c46.ExaminationFlowService.application.service.queue.dto;

public record CompleteItemDto(
        String examId,
        String doctorId,
        String serviceId,
        String data
) {
}

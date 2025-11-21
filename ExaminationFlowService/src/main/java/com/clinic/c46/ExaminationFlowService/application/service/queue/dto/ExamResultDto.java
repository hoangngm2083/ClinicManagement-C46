package com.clinic.c46.ExaminationFlowService.application.service.queue.dto;


import lombok.Builder;

@Builder
public record ExamResultDto(String doctorId, String examId, String serviceId, String data) {
}

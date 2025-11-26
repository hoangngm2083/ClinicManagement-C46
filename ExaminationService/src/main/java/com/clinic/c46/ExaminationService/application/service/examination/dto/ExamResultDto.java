package com.clinic.c46.ExaminationService.application.service.examination.dto;


import lombok.Builder;

@Builder
public record ExamResultDto(String doctorId, String examId, String serviceId, String data) {
}

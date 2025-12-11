package com.clinic.c46.ExaminationService.application.service.examination.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record ExamResultDto(String doctorId, String examId, String serviceId, JsonNode data) {

}

package com.clinic.c46.CommonService.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record MedicalResultDto(String doctorId, String serviceId, String serviceName, JsonNode data, String pdfUrl,
        String status, String doctorName, JsonNode serviceFormTemplate) {
}

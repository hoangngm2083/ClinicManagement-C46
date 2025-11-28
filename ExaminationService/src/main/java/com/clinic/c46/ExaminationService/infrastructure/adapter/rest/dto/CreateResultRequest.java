package com.clinic.c46.ExaminationService.infrastructure.adapter.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateResultRequest(

                @NotBlank(message = "Mã dịch vụ không được trống") String serviceId,

                JsonNode data) {
}

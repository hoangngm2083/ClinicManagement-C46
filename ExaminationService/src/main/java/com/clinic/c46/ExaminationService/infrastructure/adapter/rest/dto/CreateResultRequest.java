package com.clinic.c46.ExaminationService.infrastructure.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateResultRequest(

        @NotBlank(message = "Mã dịch vụ không được trống") String serviceId,

        @NotBlank(message = "Dữ liệu không được trống") String data) {
}

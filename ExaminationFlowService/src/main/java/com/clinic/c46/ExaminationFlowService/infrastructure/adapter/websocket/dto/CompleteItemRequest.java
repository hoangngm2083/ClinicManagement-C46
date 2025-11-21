package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteItemRequest(@NotBlank String queueItemId, @NotBlank String examId, @NotBlank String serviceId,
                                  @NotBlank String data) {

}

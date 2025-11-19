package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto;

import jakarta.validation.constraints.NotBlank;


public record TakeNextItemRequest(@NotBlank String queueId) {
}

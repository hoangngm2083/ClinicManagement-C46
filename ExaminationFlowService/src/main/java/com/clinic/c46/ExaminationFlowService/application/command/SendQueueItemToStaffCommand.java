package com.clinic.c46.ExaminationFlowService.application.command;

import lombok.Builder;

@Builder
public record SendQueueItemToStaffCommand(String staffId, String queueId, String queueItemId) {
}

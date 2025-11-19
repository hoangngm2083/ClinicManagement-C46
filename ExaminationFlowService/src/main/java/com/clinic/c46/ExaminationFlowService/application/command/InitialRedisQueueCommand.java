package com.clinic.c46.ExaminationFlowService.application.command;

import java.util.List;

public record InitialRedisQueueCommand(List<String> departmentIds) {
}

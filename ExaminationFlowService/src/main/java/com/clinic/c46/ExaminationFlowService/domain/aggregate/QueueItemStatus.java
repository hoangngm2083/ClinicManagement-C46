package com.clinic.c46.ExaminationFlowService.domain.aggregate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QueueItemStatus {
    WAITING(0), IN_PROGRESS(1), COMPLETED(2);
    private final int value;
}

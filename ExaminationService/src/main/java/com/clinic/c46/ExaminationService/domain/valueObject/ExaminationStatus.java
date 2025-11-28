package com.clinic.c46.ExaminationService.domain.valueObject;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExaminationStatus {
    PENDING(0), COMPLETED(1);

    private final int value;
}

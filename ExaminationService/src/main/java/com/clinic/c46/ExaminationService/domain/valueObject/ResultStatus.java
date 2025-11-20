package com.clinic.c46.ExaminationService.domain.valueObject;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ResultStatus {
    CREATED(0), SIGNED(1), REMOVED(2);
    private final int value;
}

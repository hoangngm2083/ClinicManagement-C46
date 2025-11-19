package com.clinic.c46.ExaminationService.domain.event;

import lombok.Builder;

@Builder
public record ExaminationDeletedEvent(String examId) {

}

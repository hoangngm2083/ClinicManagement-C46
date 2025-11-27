package com.clinic.c46.CommonService.event.examination;

import lombok.Builder;

@Builder
public record ExaminationCompletedEvent(String examinationId, String status) {
}

package com.clinic.c46.CommonService.event.examination;

import lombok.Builder;

@Builder
public record ExaminationCreatedEvent(String examinationId, String patientId) {

}

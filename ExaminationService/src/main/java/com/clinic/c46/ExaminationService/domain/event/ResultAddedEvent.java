package com.clinic.c46.ExaminationService.domain.event;

import com.clinic.c46.ExaminationService.domain.valueObject.MedicalResult;
import lombok.Builder;

@Builder
public record ResultAddedEvent(String examinationId, MedicalResult medicalResult) {

}

package com.clinic.c46.ExaminationService.domain.event;

import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import lombok.Builder;

@Builder
public record ResultStatusUpdatedEvent(String examId, String serviceId, ResultStatus newStatus) {

}

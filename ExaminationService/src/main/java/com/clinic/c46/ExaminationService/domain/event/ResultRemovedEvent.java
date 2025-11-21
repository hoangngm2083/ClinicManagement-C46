package com.clinic.c46.ExaminationService.domain.event;

import lombok.Builder;

@Builder
public record ResultRemovedEvent(String examId, String serviceId) {

}

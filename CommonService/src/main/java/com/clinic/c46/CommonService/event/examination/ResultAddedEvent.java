package com.clinic.c46.CommonService.event.examination;

import lombok.Builder;

@Builder
public record ResultAddedEvent(String examinationId, String doctorId, String serviceId, String data) {

}

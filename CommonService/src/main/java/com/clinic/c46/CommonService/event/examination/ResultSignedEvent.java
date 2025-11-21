package com.clinic.c46.CommonService.event.examination;

public record ResultSignedEvent(String examinationId, String doctorId, String serviceId) {
}

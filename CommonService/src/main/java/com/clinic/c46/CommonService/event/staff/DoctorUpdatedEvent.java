package com.clinic.c46.CommonService.event.staff;

import lombok.Builder;

@Builder
public record DoctorUpdatedEvent(String doctorId, String doctorName, String eSignature) {
}

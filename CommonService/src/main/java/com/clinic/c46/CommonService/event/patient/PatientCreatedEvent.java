package com.clinic.c46.CommonService.event.patient;


import lombok.Builder;

@Builder
public record PatientCreatedEvent(String patientId, String name, String phone, String email) {
}

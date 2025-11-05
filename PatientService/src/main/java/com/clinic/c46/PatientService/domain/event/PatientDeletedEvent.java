package com.clinic.c46.PatientService.domain.event;


import lombok.Builder;

@Builder
public record PatientDeletedEvent (String patientId) {
}

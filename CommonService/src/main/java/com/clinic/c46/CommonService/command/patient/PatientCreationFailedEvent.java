package com.clinic.c46.CommonService.command.patient;


import lombok.Builder;

@Builder
public record PatientCreationFailedEvent(

        String patientId

) {
}

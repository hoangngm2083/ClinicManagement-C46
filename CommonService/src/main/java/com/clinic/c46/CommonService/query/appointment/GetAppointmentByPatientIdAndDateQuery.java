package com.clinic.c46.CommonService.query.appointment;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetAppointmentByPatientIdAndDateQuery(String patientId, LocalDate date) {
}


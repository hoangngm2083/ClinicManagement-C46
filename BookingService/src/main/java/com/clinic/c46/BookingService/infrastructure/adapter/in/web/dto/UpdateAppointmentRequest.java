package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateAppointmentRequest {
    private int shift;
    private LocalDate date;
    private String patientName;
    private String patientId;
    private String medicalPackageId;
    private String state;
}

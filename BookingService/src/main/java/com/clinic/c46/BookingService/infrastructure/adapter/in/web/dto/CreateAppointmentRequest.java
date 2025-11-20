package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;

import lombok.Data;

@Data
public class CreateAppointmentRequest {
    private String patientId;
    private String slotId;
}

package com.clinic.c46.BookingService.domain.event;


import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AppointmentCreatedEvent(
        String appointmentId,
        String patientId,
        String slotId
) {
}

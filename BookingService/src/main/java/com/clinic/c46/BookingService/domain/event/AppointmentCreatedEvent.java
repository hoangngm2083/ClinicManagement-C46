package com.clinic.c46.BookingService.domain.event;


import lombok.Builder;

@Builder
public record AppointmentCreatedEvent(String appointmentId, String patientId, String slotId, String state) {
}

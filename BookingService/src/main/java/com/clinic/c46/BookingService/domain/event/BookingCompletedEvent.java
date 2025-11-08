package com.clinic.c46.BookingService.domain.event;

import lombok.Builder;

@Builder
public record BookingCompletedEvent(String bookingId, String appointmentId, String patientId) {
}

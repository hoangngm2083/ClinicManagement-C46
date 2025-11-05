package com.clinic.c46.BookingService.application.saga.event;

import lombok.Builder;

@Builder
public record BookingRejectedEvent(String bookingId, String reason) {
}

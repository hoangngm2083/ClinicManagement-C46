package com.clinic.c46.BookingService.domain.valueObject;

import lombok.Builder;

@Builder
public record LockedSlot(String fingerprint, String bookingId) {
}

package com.clinic.c46.BookingService.domain.event;


import lombok.Builder;


@Builder
public record SlotLockedEvent(String bookingId, String slotId, String fingerprint, String name, String phone,
                              String email) {
}

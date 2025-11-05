package com.clinic.c46.BookingService.domain.event;


import lombok.Builder;

@Builder
public record SlotReleasedEvent(
        String slotId,
        String fingerprint
        ) {
}

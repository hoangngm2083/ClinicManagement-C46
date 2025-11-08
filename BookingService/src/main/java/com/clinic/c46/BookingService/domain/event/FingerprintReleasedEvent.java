package com.clinic.c46.BookingService.domain.event;


import lombok.Builder;

@Builder
public record FingerprintReleasedEvent(String slotId, String fingerprint) {
}

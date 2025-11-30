package com.clinic.c46.BookingService.domain.event;

import lombok.Builder;

@Builder
public record SlotMaxQuantityUpdatedEvent(
        String slotId,
        int oldMaxQuantity,
        int newMaxQuantity) {
}

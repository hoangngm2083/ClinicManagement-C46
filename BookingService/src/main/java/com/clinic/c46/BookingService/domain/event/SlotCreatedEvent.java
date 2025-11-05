package com.clinic.c46.BookingService.domain.event;
import lombok.Builder;

import java.time.LocalDate;


@Builder
public record SlotCreatedEvent(
        String slotId,
        String medicalPackageId,
        LocalDate date,
        int shift,
        int maxQuantity
) {
}
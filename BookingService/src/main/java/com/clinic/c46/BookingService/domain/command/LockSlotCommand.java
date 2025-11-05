package com.clinic.c46.BookingService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record LockSlotCommand(@TargetAggregateIdentifier String slotId, String bookingId, String fingerprint,
                              String name, String email, String phone) {
}

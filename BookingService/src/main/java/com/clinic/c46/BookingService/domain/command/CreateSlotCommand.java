package com.clinic.c46.BookingService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDate;


@Builder
public record CreateSlotCommand(@TargetAggregateIdentifier String slotId, LocalDate date, int shift,
                                String medicalPackageId, int maxQuantity) {
}

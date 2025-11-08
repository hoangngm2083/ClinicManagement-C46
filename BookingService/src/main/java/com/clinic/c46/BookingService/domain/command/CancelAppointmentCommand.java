package com.clinic.c46.BookingService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CancelAppointmentCommand(@TargetAggregateIdentifier String appointmentId) {
}

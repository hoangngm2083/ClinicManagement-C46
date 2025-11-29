package com.clinic.c46.CommonService.command.notification;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record SendAppointmentInfoCommand(
        @TargetAggregateIdentifier String notificationId,
        String appointmentId) {
}

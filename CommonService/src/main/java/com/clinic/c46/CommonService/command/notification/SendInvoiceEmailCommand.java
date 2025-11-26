package com.clinic.c46.CommonService.command.notification;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record SendInvoiceEmailCommand(
        @TargetAggregateIdentifier String notificationId,
        String invoiceId,
        String recipientEmail) {
}

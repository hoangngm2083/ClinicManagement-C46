package com.clinic.c46.PaymentService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Builder
public record ConfirmTransactionCommand(@TargetAggregateIdentifier String transactionId, String gatewayTransactionId,
                                        boolean paymentSuccessful) {
}

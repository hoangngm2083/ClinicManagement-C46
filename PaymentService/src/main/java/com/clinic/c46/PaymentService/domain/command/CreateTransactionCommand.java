package com.clinic.c46.PaymentService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;


@Builder
public record CreateTransactionCommand(@TargetAggregateIdentifier String transactionId, String invoiceId,
                                       BigDecimal amount, String staffId, String paymentMethod) {
}

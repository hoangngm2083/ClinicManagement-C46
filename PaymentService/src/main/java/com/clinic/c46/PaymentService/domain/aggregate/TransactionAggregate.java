package com.clinic.c46.PaymentService.domain.aggregate;

import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

@Aggregate
@NoArgsConstructor
public class TransactionAggregate {
    @AggregateIdentifier
    private String transactionId;
    private String invoiceId;
    private String staffId;
    private String paymentMethodId;
    private BigDecimal amount;
    private TransactionStatus status;
    private String gatewayTransactionId;

}

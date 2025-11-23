package com.clinic.c46.PaymentService.domain.aggregate;

import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class InvoiceAggregate {
    @AggregateIdentifier
    private String invoiceId;
    private String medicalFormId;
    private String transactionId;
    private InvoiceStatus status;

}

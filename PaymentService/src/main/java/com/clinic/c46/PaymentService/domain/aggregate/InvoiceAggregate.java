package com.clinic.c46.PaymentService.domain.aggregate;

import com.clinic.c46.CommonService.command.payment.CreateInvoiceCommand;
import com.clinic.c46.PaymentService.domain.command.MarkInvoicePaidCommand;
import com.clinic.c46.CommonService.event.payment.InvoiceCreatedEvent;
import com.clinic.c46.PaymentService.domain.event.InvoicePaidEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

@Aggregate
@NoArgsConstructor
public class InvoiceAggregate {
    @AggregateIdentifier
    private String invoiceId;
    private String medicalFormId;
    private String transactionId;
    private InvoiceStatus status;
    private BigDecimal snapshotPrice;

    @CommandHandler
    public InvoiceAggregate(CreateInvoiceCommand command) {
        AggregateLifecycle.apply(InvoiceCreatedEvent.builder()
                .invoiceId(command.invoiceId())
                .medicalFormId(command.medicalFormId())
                .snapshotPrice(command.snapshotPrice())
                .build());
    }

    @EventSourcingHandler
    public void on(InvoiceCreatedEvent event) {
        this.invoiceId = event.invoiceId();
        this.medicalFormId = event.medicalFormId();
        this.snapshotPrice = event.snapshotPrice();
        this.status = InvoiceStatus.PENDING_PAYMENT;
    }

    @CommandHandler
    public void handle(MarkInvoicePaidCommand command) {
        if (InvoiceStatus.PAYED.equals(status)) {
            throw new IllegalStateException(String.format("Hóa đơn (%s) đã được thanh toán", command.invoiceId()));
        }
        AggregateLifecycle.apply(InvoicePaidEvent.builder()
                .invoiceId(command.invoiceId())
                .transactionId(command.transactionId())
                .build());
    }

    @EventSourcingHandler
    public void on(InvoicePaidEvent event) {
        this.transactionId = event.getTransactionId();
        this.status = InvoiceStatus.PAYED;
    }
}

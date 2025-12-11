package com.clinic.c46.PaymentService.domain.aggregate;

import com.clinic.c46.CommonService.event.payment.TransactionCompletedEvent;
import com.clinic.c46.PaymentService.domain.command.ConfirmTransactionCommand;
import com.clinic.c46.PaymentService.domain.command.CreateTransactionCommand;
import com.clinic.c46.PaymentService.domain.event.TransactionCreatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;

@Aggregate
@NoArgsConstructor
public class TransactionAggregate {
    @AggregateIdentifier
    private String transactionId;
    private String invoiceId;
    private String staffId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private TransactionStatus status;
    private String gatewayTransactionId;

    @CommandHandler
    public TransactionAggregate(CreateTransactionCommand command) {
        AggregateLifecycle.apply(TransactionCreatedEvent.builder()
                .transactionId(command.transactionId())
                .invoiceId(command.invoiceId())
                .staffId(command.staffId())
                .amount(command.amount())
                .paymentMethod(command.paymentMethod())
                .build());
    }

    @EventSourcingHandler
    public void on(TransactionCreatedEvent event) {
        this.transactionId = event.transactionId();
        this.invoiceId = event.invoiceId();
        this.staffId = event.staffId();
        this.amount = event.amount();
        this.paymentMethod = PaymentMethod.valueOf(event.paymentMethod());
        this.status = TransactionStatus.PENDING;
    }

    @CommandHandler
    public void handle(ConfirmTransactionCommand command) {

        if (!this.status.equals(TransactionStatus.PENDING)) {
            throw new IllegalStateException("Giao dịch đã hoàn thành!");
        }

        String newStatus = command.paymentSuccessful() ? TransactionStatus.SUCCEEDED.name() : TransactionStatus.FAILED.name();
        AggregateLifecycle.apply(TransactionCompletedEvent.builder()
                .transactionId(command.transactionId())
                .invoiceId(this.invoiceId)
                .gatewayTransactionId(command.gatewayTransactionId())
                .transactionStatus(newStatus)
                .build());

    }

    @EventSourcingHandler
    public void on(TransactionCompletedEvent event) {
        this.gatewayTransactionId = event.gatewayTransactionId();
        this.status = TransactionStatus.valueOf(event.transactionStatus());
    }
}

package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import com.clinic.c46.PaymentService.domain.event.TransactionCompletedEvent;
import com.clinic.c46.PaymentService.domain.event.TransactionCreatedEvent;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.TransactionProjection;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionProjector {
    private final TransactionRepository transactionRepository;

    @EventHandler
    public void on(TransactionCreatedEvent event) {


        TransactionProjection projection = new TransactionProjection();
        projection.setId(event.transactionId());
        projection.setInvoiceId(event.invoiceId());
        projection.setStaffId(event.staffId());
        projection.setAmount(event.amount());
        projection.setPaymentMethod(PaymentMethod.valueOf(event.paymentMethod()));
        projection.setStatus(TransactionStatus.PENDING);
        projection.markCreated();
        transactionRepository.save(projection);
    }

    @EventHandler
    public void on(TransactionCompletedEvent event) {
        transactionRepository.findById(event.transactionId())
                .ifPresent(transaction -> {
                    transaction.setStatus(TransactionStatus.valueOf(event.transactionStatus()));
                    transaction.setGatewayTransactionId(event.gatewayTransactionId());
                    transaction.markUpdated();
                    transactionRepository.save(transaction);
                });
    }

}

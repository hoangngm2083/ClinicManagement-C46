package com.clinic.c46.PaymentService.application.handler;

import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import com.clinic.c46.PaymentService.domain.command.MarkInvoicePaidCommand;
import com.clinic.c46.CommonService.event.payment.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventHandler {
    private final CommandGateway commandGateway;

    @EventHandler
    public void on(TransactionCompletedEvent event) {
        // 1. Check Transaction status (this logic is OK)
        TransactionStatus transactionStatus = TransactionStatus.valueOf(event.transactionStatus());
        if (transactionStatus.equals(TransactionStatus.FAILED)) {
            return;
        }

        // 2. Send Command immediately.
        // Aggregate (Command Handler) will automatically check the current state.
        MarkInvoicePaidCommand command = new MarkInvoicePaidCommand(event.invoiceId(), event.transactionId());

        commandGateway.send(command)
                .whenComplete((__, throwable) -> {
                    if (throwable != null) {
                        // Log an explicit error if Command cannot be sent or Aggregate rejects
                        log.error("Failed to mark invoice paid command for InvoiceId: {}", event.invoiceId(),
                                throwable);
                    }
                });
    }
}

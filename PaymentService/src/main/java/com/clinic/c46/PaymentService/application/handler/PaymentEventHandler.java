package com.clinic.c46.PaymentService.application.handler;

import com.clinic.c46.CommonService.command.notification.SendInvoiceEmailCommand;
import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import com.clinic.c46.CommonService.event.payment.TransactionCompletedEvent;
import com.clinic.c46.CommonService.query.invoice.GetInvoiceDetailsByIdQuery;
import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import com.clinic.c46.PaymentService.domain.command.MarkInvoicePaidCommand;
import com.clinic.c46.PaymentService.domain.event.InvoicePaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventHandler {
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

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

    @EventHandler
    public void on(InvoicePaidEvent event) {
        log.info("[PaymentEventHandler] Invoice paid: {}. Sending email...", event.getInvoiceId());

        GetInvoiceDetailsByIdQuery query = new GetInvoiceDetailsByIdQuery(event.getInvoiceId());
        queryGateway.query(query, ResponseTypes.optionalInstanceOf(InvoiceDetailsDto.class))
                .thenAccept(invoiceOpt -> {
                    if (invoiceOpt.isEmpty()) {
                        log.warn("[PaymentEventHandler] Invoice not found: {}", event.getInvoiceId());
                        return;
                    }

                    InvoiceDetailsDto invoice = invoiceOpt.get();
                    if (invoice.patientEmail() == null || invoice.patientEmail().isEmpty()) {
                        log.warn("[PaymentEventHandler] Patient email is missing for invoice: {}",
                                event.getInvoiceId());
                        return;
                    }

                    String notificationId = UUID.randomUUID().toString();
                    SendInvoiceEmailCommand command = SendInvoiceEmailCommand.builder()
                            .notificationId(notificationId)
                            .invoiceId(event.getInvoiceId())
                            .recipientEmail(invoice.patientEmail())
                            .build();

                    commandGateway.send(command)
                            .whenComplete((__, throwable) -> {
                                if (throwable != null) {
                                    log.error(
                                            "[PaymentEventHandler] Failed to send invoice email command for InvoiceId: {}",
                                            event.getInvoiceId(), throwable);
                                }
                            });
                    log.info("[PaymentEventHandler] Sent email command for invoice: {}", event.getInvoiceId());
                });
    }
}

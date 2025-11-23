package com.clinic.c46.PaymentService.application.service;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.staff.ExistsStaffByIdQuery;
import com.clinic.c46.PaymentService.application.dto.*;
import com.clinic.c46.PaymentService.application.query.GetInvoiceByIdQuery;
import com.clinic.c46.PaymentService.application.query.GetTransactionByIdQuery;
import com.clinic.c46.PaymentService.domain.aggregate.InvoiceStatus;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import com.clinic.c46.PaymentService.domain.command.ConfirmTransactionCommand;
import com.clinic.c46.PaymentService.domain.command.CreateTransactionCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final PaymentGatewayFactory paymentGatewayFactory;

    @Override
    public CompletableFuture<CreateTransactionResponse> createTransaction(CreateTransactionRequest request,
            String clientIp) {
        // 1. Prepare Async Checks (Run in parallel to optimize performance)
        CompletableFuture<Void> staffCheck = checkStaffExists(request.getStaffId());
        CompletableFuture<InvoiceDto> invoiceFetch = getInvoice(request.getInvoiceId());

        // 2. Combine the results and process the main logic
        return staffCheck.thenCombine(invoiceFetch, (__, invoice) -> invoice)
                .thenCompose(invoice -> {
                    if (InvoiceStatus.valueOf(invoice.status()) == InvoiceStatus.PAYED) {
                        throw new IllegalStateException("Hóa đơn này đã được thanh toán!");
                    }
                    return processTransactionPayment(invoice, request, clientIp);
                });
    }

    @Override
    public CompletableFuture<Void> confirmTransaction(ConfirmTransactionRequest request) {

        CompletableFuture<Optional<TransactionDto>> transFeature = queryGateway.query(
                new GetTransactionByIdQuery(request.transactionId()),
                ResponseTypes.optionalInstanceOf(TransactionDto.class));

        return transFeature.thenCompose((transactionDtoOpt) -> {

            TransactionDto transactionDto = transactionDtoOpt.orElseThrow(
                    () -> new ResourceNotFoundException("Giao dịch"));

            if (TransactionStatus.valueOf(transactionDto.getStatus()) != TransactionStatus.PENDING) {
                throw new IllegalStateException("Giao dịch đã được hoàn thành trước đó!");
            }

            ConfirmTransactionCommand confirmTransactionCommand = new ConfirmTransactionCommand(request.transactionId(),
                    request.gatewayTransactionId(), request.isSuccess());

            return commandGateway.send(confirmTransactionCommand);
        });


    }


    private CompletableFuture<Void> checkStaffExists(String staffId) {
        return queryGateway.query(new ExistsStaffByIdQuery(staffId), Boolean.class)
                .thenAccept(isExists -> {
                    if (Boolean.FALSE.equals(isExists)) {
                        throw new ResourceNotFoundException("Nhân viên");
                    }
                });
    }

    private CompletableFuture<InvoiceDto> getInvoice(String invoiceId) {
        return queryGateway.query(new GetInvoiceByIdQuery(invoiceId),
                        ResponseTypes.optionalInstanceOf(InvoiceDto.class))
                .thenApply(opt -> opt.orElseThrow(() -> new ResourceNotFoundException("Hóa đơn")));
    }

    private CompletableFuture<CreateTransactionResponse> processTransactionPayment(InvoiceDto invoice,
            CreateTransactionRequest request, String clientIp) {
        String transactionId = UUID.randomUUID()
                .toString();
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());

        CreateTransactionCommand createCommand = CreateTransactionCommand.builder()
                .transactionId(transactionId)
                .invoiceId(request.getInvoiceId())
                .staffId(request.getStaffId())
                .amount(invoice.totalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

        if (method == PaymentMethod.CASH) {
            return handleCashPayment(createCommand, transactionId);
        }
        return handleGatewayPayment(createCommand, transactionId, invoice, clientIp, method);

    }

    private CompletableFuture<CreateTransactionResponse> handleCashPayment(CreateTransactionCommand createCommand,
            String transactionId) {
        ConfirmTransactionCommand confirmCommand = ConfirmTransactionCommand.builder()
                .transactionId(transactionId)
                .gatewayTransactionId("")
                .paymentSuccessful(true)
                .build();

        return commandGateway.send(createCommand)
                .thenCompose(__ -> commandGateway.send(confirmCommand))
                .thenApply(__ -> CreateTransactionResponse.builder()
                        .transactionId(transactionId)
                        .paymentUrl("")
                        .build());
    }

    private CompletableFuture<CreateTransactionResponse> handleGatewayPayment(CreateTransactionCommand createCommand,
            String transactionId, InvoiceDto invoice, String clientIp, PaymentMethod method) {

        PaymentGateway gateway = paymentGatewayFactory.get(method);
        String paymentUrl = gateway.generateURL(transactionId, invoice.totalAmount(), clientIp);

        return commandGateway.send(createCommand)
                .thenApply(__ -> CreateTransactionResponse.builder()
                        .transactionId(transactionId)
                        .paymentUrl(paymentUrl)
                        .build());
    }
}

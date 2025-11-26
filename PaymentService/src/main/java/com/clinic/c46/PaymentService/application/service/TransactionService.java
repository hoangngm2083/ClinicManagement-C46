package com.clinic.c46.PaymentService.application.service;

import com.clinic.c46.PaymentService.application.dto.ConfirmTransactionRequest;
import com.clinic.c46.PaymentService.application.dto.CreateTransactionRequest;
import com.clinic.c46.PaymentService.application.dto.CreateTransactionResponse;

import java.util.concurrent.CompletableFuture;

public interface TransactionService {
    CompletableFuture<CreateTransactionResponse> createTransaction(String staffId, CreateTransactionRequest request,
            String clientIp);

    CompletableFuture<Void> confirmTransaction(ConfirmTransactionRequest request);
}

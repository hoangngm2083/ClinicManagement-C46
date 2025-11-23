package com.clinic.c46.PaymentService.infrastructure.adapter.controller;

import com.clinic.c46.PaymentService.application.dto.ConfirmTransactionRequest;
import com.clinic.c46.PaymentService.application.service.PaymentGateway;
import com.clinic.c46.PaymentService.application.service.PaymentGatewayFactory;
import com.clinic.c46.PaymentService.application.service.TransactionService;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payment/ipn")
@RequiredArgsConstructor
@Slf4j
public class IPNController {

    private final TransactionService transactionService;
    private final PaymentGatewayFactory paymentGatewayFactory;

    @GetMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> handle(@RequestParam Map<String, String> params) {
        String method = params.get("gateway");

        // 1. Validation cơ bản (Fail-fast)
        if (method == null || method.isBlank()) {
            return CompletableFuture.completedFuture(buildErrorResponse("Thiếu trường gateway"));
        }

        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.completedFuture(buildErrorResponse("Gateway không hợp lệ"));
        }

        // 2. Chuẩn bị params
        Map<String, String> sanitizedParams = new HashMap<>(params);
        sanitizedParams.remove("gateway");
        PaymentGateway gateway = paymentGatewayFactory.get(pm);

        // 3. Async Chain: Gateway -> TransactionService -> Response
        return gateway.handleWebhook(sanitizedParams)
                .thenCompose(webhookResult -> {
                    // Nếu checksum sai, dừng lại ngay hoặc log lỗi
                    if (!webhookResult.validSignature()) {
                        // Tùy nghiệp vụ: Return 200 OK nhưng báo lỗi code, hay throw exception
                        return CompletableFuture.completedFuture(false);
                    }

                    ConfirmTransactionRequest req = ConfirmTransactionRequest.builder()
                            .transactionId(webhookResult.transactionId())
                            .gatewayTransactionId(webhookResult.gatewayTransactionId())
                            .isSuccess(webhookResult.success()) // Payment Success?
                            .build();

                    // Gọi Service xác nhận Transaction (Giả sử service trả về CompletableFuture<Void> hoặc Object)
                    return transactionService.confirmTransaction(req)
                            .thenApply(ignored -> webhookResult.success());
                })
                .thenApply(this::buildSuccessResponse)
                .exceptionally(ex -> {
                    log.error("Error processing IPN", ex);
                    return buildErrorResponse("Internal Server Error: " + ex.getMessage());
                });
    }

    // --- Helpers (Pure Functions) ---

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(boolean success) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");

        Map<String, String> payload = new HashMap<>();
        payload.put("RspCode", "00");
        payload.put("Message", success ? "Confirm Success" : "Confirm Success (Payment Failed)");

        body.put("payload", payload);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String msg) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", msg);
        return ResponseEntity.badRequest()
                .body(body);
    }
}
package com.clinic.c46.PaymentService.infrastructure.adapter.controller;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.PaymentService.application.dto.CreateTransactionRequest;
import com.clinic.c46.PaymentService.application.dto.CreateTransactionResponse;
import com.clinic.c46.PaymentService.application.dto.TransactionDto;
import com.clinic.c46.PaymentService.application.dto.TransactionStatusDto;
import com.clinic.c46.PaymentService.application.query.GetTransactionByIdQuery;
import com.clinic.c46.PaymentService.application.query.GetTransactionStatusQuery;
import com.clinic.c46.PaymentService.application.service.TransactionServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payment/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionServiceImpl transactionServiceImpl;
    private final QueryGateway queryGateway;

    @PostMapping
    public CompletableFuture<ResponseEntity<CreateTransactionResponse>> createTransaction(
            @RequestBody CreateTransactionRequest request, HttpServletRequest servletRequest) {
        log.info("Creating transaction for invoice: {}", request.getInvoiceId());
        String clientIp = this.getClientIp(servletRequest);
        return transactionServiceImpl.createTransaction(request, clientIp)
                .thenApply(createTransactionResponse -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(createTransactionResponse));

    }


    @GetMapping("/{transactionId}")
    public CompletableFuture<ResponseEntity<TransactionDto>> getTransaction(@PathVariable String transactionId) {
        log.info("Getting transaction: {}", transactionId);

        return queryGateway.query(new GetTransactionByIdQuery(transactionId),
                        ResponseTypes.optionalInstanceOf(TransactionDto.class))
                .thenApply(optionalTransactionDto -> {
                    TransactionDto transactionDto = optionalTransactionDto.orElseThrow(
                            () -> new ResourceNotFoundException("Giao dịch"));

                    return ResponseEntity.ok(transactionDto);
                });
    }


    @GetMapping("/{transactionId}/status")
    public CompletableFuture<ResponseEntity<TransactionStatusDto>> getTransactionStatus(
            @PathVariable String transactionId) {
        log.info("Getting transaction status: {}", transactionId);

        return queryGateway.query(new GetTransactionStatusQuery(transactionId),
                        ResponseTypes.optionalInstanceOf(TransactionStatusDto.class))
                .thenApply(result -> {
                    TransactionStatusDto transactionStatusDto = result.orElseThrow(
                            () -> new ResourceNotFoundException("Giao dịch"));
                    return ResponseEntity.ok(transactionStatusDto);
                });
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For"); // header phổ biến của proxy
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP"); // một số proxy dùng header này
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // fallback: IP của request
        }
        // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }


}

package com.clinic.c46.PaymentService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Transaction response in API responses
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private String transactionId;
    private String invoiceId;
    private String staffId;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String gatewayTransactionId;
}

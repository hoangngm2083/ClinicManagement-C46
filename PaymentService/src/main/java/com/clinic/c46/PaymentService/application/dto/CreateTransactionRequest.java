package com.clinic.c46.PaymentService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CreateTransaction HTTP request from receptionist
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTransactionRequest {
    private String invoiceId;
    private String paymentMethod;
}

package com.clinic.c46.PaymentService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for VNPay IPN callback webhook payload
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VNPayIpnRequestDto {
    private String vnp_TmnCode;
    private String vnp_Amount;
    private String vnp_BankCode;
    private String vnp_BankTranNo;
    private String vnp_CardType;
    private String vnp_OrderInfo;
    private String vnp_OrderType;
    private String vnp_PayDate;
    private String vnp_ResponseCode;
    private String vnp_TxnRef;
    private String vnp_TransactionNo;
    private String vnp_TransactionStatus;
    private String vnp_SecureHash;
    
    // Additional map for any extra parameters
    private Map<String, String> additionalParams;
}

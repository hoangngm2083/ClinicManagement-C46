package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_view")
public class TransactionProjection {
    @Id
    private String id;
    private String invoiceId;
    private String staffId;
    private String paymentMethodId;
    private BigDecimal amount;
    private TransactionStatus status;
    private String gatewayTransactionId;
}

package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_view")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProjection extends BaseView {
    @Id
    private String id;
    private String invoiceId;
    private String staffId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private TransactionStatus status;
    private String gatewayTransactionId;
}

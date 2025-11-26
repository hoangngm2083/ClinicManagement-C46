package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection;


import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.PaymentService.domain.aggregate.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "invoice_view")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class InvoiceProjection extends BaseView {
    @Id
    private String id;

    private String patientId;

    private BigDecimal totalAmount;

    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING_PAYMENT;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_medical_packages", joinColumns = @JoinColumn(name = "invoice_id"))
    private Set<MedicalPackageRep> medicalPackages = new HashSet<>();
}


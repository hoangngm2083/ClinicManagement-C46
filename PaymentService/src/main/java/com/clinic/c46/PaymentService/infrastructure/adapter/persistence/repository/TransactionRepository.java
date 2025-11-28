package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.TransactionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionProjection, String>, JpaSpecificationExecutor<TransactionProjection> {
    boolean existsByInvoiceId(String invoiceId);
}

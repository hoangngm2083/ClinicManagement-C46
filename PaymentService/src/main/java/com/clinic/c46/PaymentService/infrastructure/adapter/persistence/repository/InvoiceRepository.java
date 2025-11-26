package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.InvoiceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceProjection, String> {
}

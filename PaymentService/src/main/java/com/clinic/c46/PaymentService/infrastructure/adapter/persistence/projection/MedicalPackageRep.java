package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MedicalPackageRep(String id, String name, BigDecimal price) {
    public boolean equals(MedicalPackageRep another) {
        return this.id.equals(another.id);
    }
}

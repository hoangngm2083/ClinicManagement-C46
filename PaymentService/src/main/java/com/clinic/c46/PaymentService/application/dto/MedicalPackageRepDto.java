package com.clinic.c46.PaymentService.application.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MedicalPackageRepDto(String id, String name, BigDecimal price) {
    public boolean equals(MedicalPackageRepDto another) {
        return this.id.equals(another.id());
    }
}
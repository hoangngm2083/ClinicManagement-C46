package com.clinic.c46.CommonService.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MedicalPackageRepDto(String id, String name, BigDecimal price, int priceVersion) {
    public boolean equals(MedicalPackageRepDto another) {
        return this.id.equals(another.id());
    }
}

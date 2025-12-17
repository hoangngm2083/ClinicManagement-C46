package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ImportCsvRequest {
    @NotBlank(message = "CSV URL is required")
    private String csvUrl;
}

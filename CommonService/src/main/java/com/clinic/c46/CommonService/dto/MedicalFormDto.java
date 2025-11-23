package com.clinic.c46.CommonService.dto;


import java.util.Set;

public record MedicalFormDto(String id, String patientId, String medicalFormStatus, String examinationId,
                             String invoiceId, Set<String> packageIds) {
}

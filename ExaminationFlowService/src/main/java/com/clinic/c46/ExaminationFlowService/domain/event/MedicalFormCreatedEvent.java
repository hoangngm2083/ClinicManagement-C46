package com.clinic.c46.ExaminationFlowService.domain.event;

import lombok.Builder;

import java.util.Set;

@Builder
public record MedicalFormCreatedEvent(String medicalFormId, String patientId, String invoiceId, String examinationId,
                                      Set<String> packageIds, String status) {
}

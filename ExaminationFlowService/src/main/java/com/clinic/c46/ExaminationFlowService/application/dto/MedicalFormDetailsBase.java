package com.clinic.c46.ExaminationFlowService.application.dto;

/**
 * Base interface for medical form details.
 * Allows type-safe handling of different medical form types based on
 * QueueItemType.
 */
public interface MedicalFormDetailsBase {

    String id();

    String medicalFormStatus();
}

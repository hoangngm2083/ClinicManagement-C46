package com.clinic.c46.ExaminationFlowService.application.service.medicalForm;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationFlowService.application.service.medicalForm.dto.CreateMedicalFormDto;
import jakarta.validation.Valid;

import java.util.concurrent.CompletableFuture;

public interface MedicalFormService {
    CompletableFuture<String> createMedicalForm(@Valid CreateMedicalFormDto dto) throws ResourceNotFoundException;
}

package com.clinic.c46.ExaminationService.application.service;

import com.clinic.c46.ExaminationService.infrastructure.adapter.web.dto.CreateMedicalResultRequest;

import java.util.concurrent.CompletableFuture;

public interface MedicalResultAppService {
    CompletableFuture<String> createMedicalResult(CreateMedicalResultRequest request);
}

package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.controller;


import com.clinic.c46.ExaminationFlowService.application.service.medicalForm.MedicalFormService;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.controller.dto.CreateMedicalFormRequest;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.MedicalFormMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Validated
@RestController
@RequestMapping("/api/medical-form")
@RequiredArgsConstructor
public class MedicalFormController {

    private final MedicalFormService medicalFormService;
    private final MedicalFormMapper medicalFormMapper;

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> createMedicalForm(
            @RequestBody @Valid CreateMedicalFormRequest request) {
        return medicalFormService.createMedicalForm(medicalFormMapper.toDto(request))
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(response));
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<String>> getAllMedicalForms(
            @RequestBody @Valid CreateMedicalFormRequest request) {
        return medicalFormService.createMedicalForm(medicalFormMapper.toDto(request))
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(response));
    }

}

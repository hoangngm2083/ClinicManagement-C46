package com.clinic.c46.ExaminationService.application.service;

import com.clinic.c46.ExaminationService.infrastructure.adapter.web.dto.CreateMedicalResultRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Validated
@RequiredArgsConstructor
public class MedicalResultAppServiceImpl implements MedicalResultAppService {

    private final CommandGateway commandGateway;


    @Override
    public CompletableFuture<String> createMedicalResult(@Valid CreateMedicalResultRequest request) {

        String resultId = UUID.randomUUID().toString();

        return null;
    }
}

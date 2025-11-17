package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;


import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServicesPagedDto;
import com.clinic.c46.MedicalPackageService.application.service.MedicalServiceService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery;
import com.clinic.c46.MedicalPackageService.domain.query.GetMedicalServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.CreateMedicalServiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medical-service")
@RequiredArgsConstructor
@Validated
public class MedicalServiceController {
    private final QueryGateway queryGateway;
    private final MedicalServiceService medicalServiceService;

    @GetMapping
    public ResponseEntity<MedicalServicesPagedDto> getMedicalServices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "keyword", required = false) String keyword) {

        GetAllMedicalServicesQuery getAllMedicalPackageQuery = GetAllMedicalServicesQuery.builder()
                .keyword(keyword)
                .page(page)
                .build();

        MedicalServicesPagedDto medicalPackageDTOs = queryGateway.query(getAllMedicalPackageQuery,
                        ResponseTypes.instanceOf(MedicalServicesPagedDto.class))
                .join();
        return ResponseEntity.ok()
                .body(medicalPackageDTOs);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createMedicalService(
            @RequestBody @Valid CreateMedicalServiceRequest bodyRequest) {


        String medicalServiceId = UUID.randomUUID()
                .toString();
        CreateMedicalServiceCommand cmd = CreateMedicalServiceCommand.builder()
                .medicalServiceId(medicalServiceId)
                .departmentId(bodyRequest.getDepartmentId())
                .name(bodyRequest.getName())
                .processingPriority(bodyRequest.getProcessingPriority())
                .description(bodyRequest.getDescription())
                .build();

        medicalServiceService.create(cmd);

        return ResponseEntity.created(URI.create("/medical-service/" + medicalServiceId))
                .body(Map.of("medicalServiceId", medicalServiceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalServiceDTO> getById(@PathVariable String id) {

        GetMedicalServiceByIdQuery query = GetMedicalServiceByIdQuery.builder()
                .medicalServiceId(id)
                .build();

        MedicalServiceDTO medicalServiceDTO = queryGateway.query(query,
                        ResponseTypes.instanceOf(MedicalServiceDTO.class))
                .join();
        return ResponseEntity.ok(medicalServiceDTO);
    }

}

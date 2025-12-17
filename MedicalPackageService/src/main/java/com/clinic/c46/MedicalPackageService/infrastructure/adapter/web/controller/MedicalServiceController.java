package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;


import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDetailsDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServicesPagedDto;
import com.clinic.c46.MedicalPackageService.application.service.MedicalServiceService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery;
import com.clinic.c46.MedicalPackageService.domain.query.GetMedicalServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.CreateMedicalServiceRequest;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.UpdateMedicalServiceRequest;
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
    private final com.clinic.c46.MedicalPackageService.application.service.BulkImportService bulkImportService;

    @GetMapping
    public ResponseEntity<MedicalServicesPagedDto> getMedicalServices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "medicalPackageId", required = false) String medicalPackageId) {

        GetAllMedicalServicesQuery getAllMedicalPackageQuery = GetAllMedicalServicesQuery.builder()
                .keyword(keyword)
                .page(page)
                .medicalPackageId(medicalPackageId)
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
                .formTemplate(bodyRequest.getFormTemplate())
                .description(bodyRequest.getDescription())
                .build();

        medicalServiceService.create(cmd);

        return ResponseEntity.created(URI.create("/medical-service/" + medicalServiceId))
                .body(Map.of("medicalServiceId", medicalServiceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalServiceDetailsDTO> getById(@PathVariable String id) {

        GetMedicalServiceByIdQuery query = GetMedicalServiceByIdQuery.builder()
                .medicalServiceId(id)
                .build();

        MedicalServiceDetailsDTO medicalServiceDTO = queryGateway.query(query,
                        ResponseTypes.instanceOf(MedicalServiceDetailsDTO.class))
                .join();
        return ResponseEntity.ok(medicalServiceDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMedicalService(@PathVariable String id,
            @RequestBody UpdateMedicalServiceRequest request) {

        UpdateMedicalServiceInfoCommand cmd = UpdateMedicalServiceInfoCommand.builder()
                .medicalServiceId(id)
                .name(request.getName())
                .description(request.getDescription())
                .departmentId(request.getDepartmentId())
                .processingPriority(request.getProcessingPriority() != null ? request.getProcessingPriority() : 0)
                .formTemplate(request.getFormTemplate())
                .build();

        medicalServiceService.update(cmd);
        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalService(@PathVariable String id) {
        DeleteMedicalServiceCommand cmd = DeleteMedicalServiceCommand.builder()
                .medicalServiceId(id)
                .build();

        medicalServiceService.delete(cmd);
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importCsv(
            @Valid @RequestBody com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.ImportCsvRequest request) {
        
        String bulkId = bulkImportService.startBulkImport("MEDICAL_SERVICE", request.getCsvUrl());
        
        return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                .body(Map.of(
                        "bulkId", bulkId,
                        "status", "ACCEPTED",
                        "message", "Bulk import started successfully"
                ));
    }

    @GetMapping("/import/{bulkId}")
    public ResponseEntity<com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView> getImportStatus(
            @PathVariable String bulkId) {
        
        com.clinic.c46.MedicalPackageService.domain.query.GetBulkImportStatusQuery query = 
                com.clinic.c46.MedicalPackageService.domain.query.GetBulkImportStatusQuery.builder()
                        .bulkId(bulkId)
                        .build();
        
        com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView status = 
                queryGateway.query(query, ResponseTypes.instanceOf(
                        com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView.class))
                        .join();
        
        return ResponseEntity.ok(status);
    }

}

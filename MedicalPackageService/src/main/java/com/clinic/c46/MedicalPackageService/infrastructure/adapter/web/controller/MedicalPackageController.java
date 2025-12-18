package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;


import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.query.medicalPackage.FindMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackageDetailDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackagesPagedDto;
import com.clinic.c46.MedicalPackageService.application.service.MedicalPackageService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.CreateMedicalPackageRequest;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.UpdateMedicalPackageInfoRequest;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.UpdateMedicalPackagePriceRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medical-package")
@RequiredArgsConstructor
public class MedicalPackageController {

    private final QueryGateway queryGateway;
    private final MedicalPackageService medicalPackageService;
    private final com.clinic.c46.MedicalPackageService.application.service.BulkImportService bulkImportService;

    @GetMapping
    public ResponseEntity<MedicalPackagesPagedDto> getMedicalPackages(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Trường sắp xếp",
                    schema = @Schema(allowableValues = {"name", "currentPriceVersion"}, defaultValue = "name")
            )
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Hướng sắp xếp: 'ASC' (tăng dần) hoặc 'DESC' (giảm dần)",
                    schema = @Schema(allowableValues = {"ASC", "DESC"}, defaultValue = "DESC")
            )
            @RequestParam(value = "sort", defaultValue = "ASC") SortDirection sort) {

        GetAllPackagesQuery getAllMedicalPackageQuery = GetAllPackagesQuery.builder()
                .page(page)
                .size(size)
                .keyword(keyword)
                .sortBy(sortBy)
                .sort(sort)
                .build();

        MedicalPackagesPagedDto medicalPackageDTOs = queryGateway.query(getAllMedicalPackageQuery,
                        ResponseTypes.instanceOf(MedicalPackagesPagedDto.class))
                .join();

        return ResponseEntity.ok()
                .body(medicalPackageDTOs);
    }

    @GetMapping("/export")
    public java.util.concurrent.CompletableFuture<ResponseEntity<byte[]>> exportMedicalPackages(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort", defaultValue = "ASC") SortDirection sort) {

        GetAllPackagesQuery query = GetAllPackagesQuery.builder()
                .page(1)
                .size(10000)
                .keyword(keyword)
                .sortBy(sortBy)
                .sort(sort)
                .build();

        return medicalPackageService.exportPackages(query)
                .thenApply(csvBytes -> ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medical_packages.csv\"")
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv")
                        .body(csvBytes));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createMedicalPackage(
            @RequestBody CreateMedicalPackageRequest bodyRequest) {
        String medicalPackageId = UUID.randomUUID()
                .toString();
        CreateMedicalPackageCommand cmd = CreateMedicalPackageCommand.builder()
                .medicalPackageId(medicalPackageId)
                .description(bodyRequest.getDescription())
                .price(bodyRequest.getPrice())
                .name(bodyRequest.getName())
                .image(bodyRequest.getImage())
                .serviceIds(bodyRequest.getServiceIds())
                .build();

        medicalPackageService.createPackage(cmd);

        return ResponseEntity.created(URI.create("/medicalPackages/" + medicalPackageId))
                .body(Map.of("medicalPackageId", medicalPackageId));

    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalPackageDetailDTO> getById(@PathVariable String id) {

        FindMedicalPackageByIdQuery query = FindMedicalPackageByIdQuery.builder()
                .medicalPackageId(id)
                .build();

        MedicalPackageDetailDTO medicalPackageDetailDTO = queryGateway.query(query,
                        ResponseTypes.instanceOf(MedicalPackageDetailDTO.class))
                .join();
        return ResponseEntity.ok(medicalPackageDetailDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMedicalPackageInfo(
            @PathVariable String id,
            @RequestBody UpdateMedicalPackageInfoRequest request) {

        UpdateMedicalPackageInfoCommand infoCmd = UpdateMedicalPackageInfoCommand.builder()
                .medicalPackageId(id)
                .name(request.getName())
                .description(request.getDescription())
                .serviceIds(request.getServiceIds())
                .image(request.getImage())
                .build();
        
        medicalPackageService.updateInfo(infoCmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateMedicalPackagePrice(
            @PathVariable String id,
            @RequestBody UpdateMedicalPackagePriceRequest request) {

        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Price is required");
        }

        UpdateMedicalPackagePriceCommand priceCmd = UpdateMedicalPackagePriceCommand.builder()
                .medicalPackageId(id)
                .newPrice(request.getPrice())
                .build();
        
        medicalPackageService.updatePrice(priceCmd);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalPackage(@PathVariable String id) {
        DeleteMedicalPackageCommand cmd = DeleteMedicalPackageCommand.builder()
                .medicalPackageId(id)
                .build();

        medicalPackageService.delete(cmd);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-retry")
    public ResponseEntity<String> testRetryMechanism(@RequestParam(defaultValue = "true") boolean shouldFail) {
        String result = ((com.clinic.c46.MedicalPackageService.application.service.MedicalPackageServiceImpl) medicalPackageService)
                .testRetryMechanism(shouldFail);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importCsv(
            @Valid @RequestBody com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.ImportCsvRequest request) {
        
        String bulkId = bulkImportService.startBulkImport("MEDICAL_PACKAGE", request.getCsvUrl());
        
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

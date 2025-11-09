package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;


import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.query.medicalPackage.FindMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackagesPagedDto;
import com.clinic.c46.MedicalPackageService.application.service.MedicalPackageService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto.CreateMedicalPackageRequest;
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

    @GetMapping
    public ResponseEntity<MedicalPackagesPagedDto> getMedicalPackages(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "keyword", required = false) String keyword) {

        GetAllPackagesQuery getAllMedicalPackageQuery = GetAllPackagesQuery.builder()
                .page(page)
                .keyword(keyword)
                .build();

        MedicalPackagesPagedDto medicalPackageDTOs = queryGateway.query(getAllMedicalPackageQuery,
                        ResponseTypes.instanceOf(MedicalPackagesPagedDto.class))
                .join();

        return ResponseEntity.ok()
                .body(medicalPackageDTOs);
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
                .serviceIds(bodyRequest.getServiceIds())
                .build();

        medicalPackageService.createPackage(cmd);

        return ResponseEntity.created(URI.create("/medicalPackages/" + medicalPackageId))
                .body(Map.of("medicalPackageId", medicalPackageId));

    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalPackageDTO> getById(@PathVariable String id) {

        FindMedicalPackageByIdQuery query = FindMedicalPackageByIdQuery.builder()
                .medicalPackageId(id)
                .build();

        MedicalPackageDTO medicalPackageDTO = queryGateway.query(query,
                        ResponseTypes.instanceOf(MedicalPackageDTO.class))
                .join();
        return ResponseEntity.ok(medicalPackageDTO);
    }

}

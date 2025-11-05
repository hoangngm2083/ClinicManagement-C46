package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web;


import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medicalPackages")
@RequiredArgsConstructor
public class MedicalPackageController {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    @GetMapping
    public ResponseEntity<Map<String, List<MedicalPackageView>>> getMedicalPackages() {

        GetAllPackagesQuery getAllMedicalPackageQuery = GetAllPackagesQuery.builder()
                .build();

        List<MedicalPackageView> medicalPackageDTOs = queryGateway.query(getAllMedicalPackageQuery,
                        ResponseTypes.multipleInstancesOf(MedicalPackageView.class))
                .join();

        return ResponseEntity.ok()
                .body(Map.of("medicalPackages", medicalPackageDTOs));
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

        commandGateway.sendAndWait(cmd);

        return ResponseEntity.created(URI.create("/medicalPackages/" + medicalPackageId))
                .body(Map.of("medicalPackageId", medicalPackageId));

    }


    @PostMapping("/services")
    public ResponseEntity<Map<String, String>> createMedicalService(
            @RequestBody CreateMedicalServiceRequest bodyRequest) {
        String medicalServiceId = UUID.randomUUID()
                .toString();
        CreateMedicalServiceCommand cmd = CreateMedicalServiceCommand.builder()
                .medicalServiceId(medicalServiceId)
                .description(bodyRequest.getDescription())
                .name(bodyRequest.getName())
                .departmentId(bodyRequest.getDepartmentId())
                .build();

        commandGateway.sendAndWait(cmd);

        return ResponseEntity.created(URI.create("/medicalServices/" + medicalServiceId))
                .body(Map.of("medicalServiceId", medicalServiceId));

    }



}

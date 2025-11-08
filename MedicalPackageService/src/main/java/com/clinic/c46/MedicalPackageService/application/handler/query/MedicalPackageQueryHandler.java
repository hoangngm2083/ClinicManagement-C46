package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.query.medicalPackage.*;
import com.clinic.c46.MedicalPackageService.application.port.out.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.port.out.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MedicalPackageQueryHandler {

    private final MedicalPackageViewRepository packageRepo;
    private final MedicalServiceViewRepository serviceRepo;

    @QueryHandler
    public List<MedicalPackageDTO> handle(GetAllPackagesQuery q) {
        return packageRepo.findAll()
                .stream()
                .map(view -> MedicalPackageDTO.builder()
                        .name(view.getName())
                        .medicalPackageId(view.getId())
                        .price(view.getPrice())
                        .description(view.getDescription())
                        .build())
                .toList();
    }

    @QueryHandler
    public MedicalPackageDTO handle(FindMedicalPackageByIdQuery q) {
        return packageRepo.findById(q.medicalPackageId())
                .map(view -> MedicalPackageDTO.builder()
                        .medicalPackageId(view.getId())
                        .price(view.getPrice())
                        .name(view.getName())
                        .description(view.getDescription())
                        .build())
                .orElse(null);
    }

    @QueryHandler
    public Set<MedicalServiceView> handle(GetServicesByPackageQuery q) {
        return packageRepo.findById(q.medicalPackageId())
                .map(MedicalPackageView::getMedicalServices)
                .orElse(Set.of());
    }

    @QueryHandler
    public List<MedicalServiceView> handle(GetAllServicesQuery q) {
        return serviceRepo.findAll();
    }

    @QueryHandler
    public MedicalServiceView handle(GetServiceByIdQuery q) {
        return serviceRepo.findById(q.medicalServiceId())
                .orElse(null);
    }
}

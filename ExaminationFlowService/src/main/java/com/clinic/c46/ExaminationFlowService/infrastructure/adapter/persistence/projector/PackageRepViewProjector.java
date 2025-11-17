package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;


import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackagePriceUpdatedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.PackageRepViewRepository;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.PackageRepView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.ServiceRepView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PackageRepViewProjector {
    private final PackageRepViewRepository packageRepViewRepository;
    private final ServiceRepViewRepository serviceRepViewRepository;

    @EventHandler
    public void handle(MedicalPackageCreatedEvent event) {
        if (packageRepViewRepository.existsById(event.medicalPackageId())) {
            log.info("[exam-flow.projection.medical-package.create.existed] [package-id: {}]",
                    event.medicalPackageId());
            return;
        }

        Set<ServiceRepView> serviceRepViews = handleGetServicesByIds(event.serviceIds());

        PackageRepView packageRepView = PackageRepView.builder()
                .id(event.medicalPackageId())
                .price(event.price())
                .services(serviceRepViews)
                .build();

        packageRepView.markCreated();
        packageRepViewRepository.save(packageRepView);
    }


    @EventHandler
    public void handle(MedicalPackageInfoUpdatedEvent event) {
        // Find resource -> throw runtime error for axon retry
        PackageRepView packageRepView = handleGetPackageById(event.medicalPackageId());

        Set<ServiceRepView> serviceRepViews = handleGetServicesByIds(event.serviceIds());

        // Update data
        packageRepView.setServices(serviceRepViews);
        packageRepView.markUpdated();
        // Save
        packageRepViewRepository.save(packageRepView);

    }

    @EventHandler
    public void handle(MedicalPackagePriceUpdatedEvent event) {
        // Find resource -> throw runtime error for axon retry
        PackageRepView packageRepView = handleGetPackageById(event.medicalPackageId());
        // Update data
        packageRepView.setPrice(event.newPrice());
        packageRepView.markUpdated();
        // Save
        packageRepViewRepository.save(packageRepView);

    }

    @EventHandler
    public void handle(MedicalPackageDeletedEvent event) {
        packageRepViewRepository.findById(event.medicalPackageId())
                .ifPresent(packageRepViewRepository::delete);
    }


    private PackageRepView handleGetPackageById(String medicalPackageId) {
        return packageRepViewRepository.findById(medicalPackageId)
                .orElseThrow(() -> new RuntimeException(
                        "[exam-flow.projection.medical-package.update.not-found] [package-id: ]" + medicalPackageId));

    }

    private Set<ServiceRepView> handleGetServicesByIds(Set<String> serviceIds) {
        Set<ServiceRepView> serviceRepViews = new HashSet<>(serviceRepViewRepository.findAllById(serviceIds));

        if (serviceRepViews.size() != serviceIds.size()) {
            handleMissingServices(serviceRepViews, serviceIds);
        }

        return serviceRepViews;

    }

    private void handleMissingServices(Set<ServiceRepView> foundServices, Set<String> requiredServiceIds) {
        Set<String> foundIds = foundServices.stream()
                .map(ServiceRepView::getId)
                .collect(Collectors.toSet());

        Set<String> missingIds = requiredServiceIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());

        log.warn("[exam-flow.projection.medical-package.create.medical-service.not-found] [missing-services: {}]",
                missingIds);
        throw new RuntimeException(
                "[exam-flow.projection.medical-package.create.medical-service.not-found] [missing-services: {}]" + missingIds);
    }

}

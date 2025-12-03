package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackagePriceUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalPackageProjection {

    private final MedicalPackageViewRepository packageRepo;
    private final MedicalServiceViewRepository serviceRepo;

    @EventHandler
    @Transactional
    public void on(MedicalPackageCreatedEvent event) {
        log.debug("Handling MedicalPackageCreatedEvent: {}", event);

        Set<MedicalServiceView> services = new HashSet<>();

        if (event.serviceIds() != null) {
            for (String serviceId : event.serviceIds()) {
                serviceRepo.findById(serviceId)
                        .ifPresent(services::add);
            }
        }

        MedicalPackageView view = MedicalPackageView.builder()
                .id(event.medicalPackageId())
                .name(event.name())
                .description(event.description())
                .price(event.price())
                .image(event.image())
                .medicalServices(services)
                .build();
        
        view.markCreated();
        packageRepo.save(view);
    }

    @EventHandler
    @Transactional
    public void on(MedicalPackagePriceUpdatedEvent event) {
        log.debug("Handling MedicalPackagePriceUpdatedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.setPrice(event.newPrice());
                    view.markUpdated();
                    packageRepo.save(view);
                });
    }

    @EventHandler
    @Transactional
    public void on(MedicalPackageInfoUpdatedEvent event) {
        log.debug("Handling MedicalPackageInfoUpdatedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    if (event.name() != null) view.setName(event.name());
                    if (event.description() != null) view.setDescription(event.description());
                    if (event.image() != null) view.setImage(event.image());

                    if (event.serviceIds() != null) {
                        Set<MedicalServiceView> services = new HashSet<>();
                        for (String serviceId : event.serviceIds()) {
                            serviceRepo.findById(serviceId)
                                    .ifPresent(services::add);
                        }
                        view.setMedicalServices(services);
                    }

                    view.markUpdated();
                    packageRepo.save(view);
                });
    }

    @EventHandler
    @Transactional
    public void on(MedicalPackageDeletedEvent event) {
        log.debug("Handling MedicalPackageDeletedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.markDeleted();
                    packageRepo.save(view);
                });
    }
}

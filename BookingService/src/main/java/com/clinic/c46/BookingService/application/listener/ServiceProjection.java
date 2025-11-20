package com.clinic.c46.BookingService.application.listener;

import com.clinic.c46.BookingService.application.repository.ServiceRepViewRepository;
import com.clinic.c46.BookingService.domain.view.ServiceRepView;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceProjection {

    private final ServiceRepViewRepository serviceRepViewRepository;

    @EventHandler
    @Transactional
    public void on(MedicalServiceCreatedEvent event) {
        // idempotency: skip if exists
        if (serviceRepViewRepository.existsById(event.medicalServiceId())) {
            return;
        }

        ServiceRepView serviceRepView = ServiceRepView.builder()
                .id(event.medicalServiceId())
                .name(event.name())
                .build();

        serviceRepViewRepository.save(serviceRepView);
        log.debug("Created ServiceRepView for service id: {}", event.medicalServiceId());
    }

    @EventHandler
    @Transactional
    public void on(MedicalServiceInfoUpdatedEvent event) {
        log.debug("Handling MedicalServiceInfoUpdatedEvent for id={}", event.medicalServiceId());

        serviceRepViewRepository.findById(event.medicalServiceId())
                .ifPresent(view -> {
                    if (event.name() != null) {
                        view.setName(event.name());
                    }
                    serviceRepViewRepository.save(view);
                });
    }

    @EventHandler
    @Transactional
    public void on(MedicalServiceDeletedEvent event) {
        log.debug("Handling MedicalServiceDeletedEvent for id={}", event.medicalServiceId());
        serviceRepViewRepository.deleteById(event.medicalServiceId());
    }
}


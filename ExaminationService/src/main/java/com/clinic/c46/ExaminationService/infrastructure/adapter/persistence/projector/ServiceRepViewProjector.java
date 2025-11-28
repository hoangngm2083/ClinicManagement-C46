package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ServiceRepView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRepViewProjector {
    private final ServiceRepViewRepository serviceRepViewRepository;

    @EventHandler
    public void handle(MedicalServiceCreatedEvent event) {
        if (serviceRepViewRepository.existsById(event.medicalServiceId())) {
            return;
        }

        ServiceRepView serviceRepView = ServiceRepView.builder()
                .id(event.medicalServiceId())
                .name(event.name())
                .formTemplate(event.formTemplate())
                .build();
        serviceRepView.markCreated();
        serviceRepViewRepository.save(serviceRepView);
    }

    @EventHandler
    public void handle(MedicalServiceInfoUpdatedEvent event) {
        ServiceRepView serviceRepView = serviceRepViewRepository.findById(event.medicalServiceId())
                .orElseThrow(() -> new RuntimeException(
                        "[examination.projection.medical-service.update.not-found] [service-id: " + event.medicalServiceId() + "]"));

        serviceRepView.setFormTemplate(event.formTemplate());
        serviceRepView.setName(event.name());
        serviceRepView.markUpdated();
        serviceRepViewRepository.save(serviceRepView);
    }

    @EventHandler
    public void handle(MedicalServiceDeletedEvent event) {
        serviceRepViewRepository.findById(event.medicalServiceId())
                .ifPresent(serviceRepViewRepository::delete);
    }
}

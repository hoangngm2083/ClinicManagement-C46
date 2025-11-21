package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;


import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.ServiceRepView;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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
                .departmentId(event.departmentId())
                .processingPriority(event.processingPriority())
                .build();
        serviceRepView.markCreated();
        serviceRepViewRepository.save(serviceRepView);
    }

    @EventHandler
    public void handle(MedicalServiceInfoUpdatedEvent event) {
        // Find resource -> throw runtime error for axon retry
        ServiceRepView serviceRepView = serviceRepViewRepository.findById(event.medicalServiceId())
                .orElseThrow(() -> new RuntimeException(
                        "[exam-flow.projection.medical-service.update.not-found] [service-id: ]" + event.medicalServiceId()));

        // Update data
        serviceRepView.setName(event.name());
        serviceRepView.setDepartmentId(event.departmentId());
        serviceRepView.setProcessingPriority(event.processingPriority());
        serviceRepView.markUpdated();

        // Save
        serviceRepViewRepository.save(serviceRepView);

    }

    @EventHandler
    public void handle(MedicalServiceDeletedEvent event) {
        serviceRepViewRepository.findById(event.medicalServiceId())
                .ifPresent(serviceRepViewRepository::delete);
    }

}

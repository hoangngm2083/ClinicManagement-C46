package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;


import com.clinic.c46.ExaminationFlowService.domain.event.MedicalFormCreatedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.MedicalFormView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.MedicalFormViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MedicalFormViewProjector {

    private final MedicalFormViewRepository medicalFormViewRepository;

    @EventHandler
    public void on(MedicalFormCreatedEvent event) {
        if (medicalFormViewRepository.existsById(event.medicalFormId())) return;

        MedicalFormView medicalFormView = MedicalFormView.builder()
                .id(event.medicalFormId())
                .examinationId(event.examinationId())
                .packageIds(event.packageIds())
                .patientId(event.patientId())
                .invoiceId(event.invoiceId())
                .medicalFormStatus(event.status())
                .build();
        medicalFormView.markCreated();
        medicalFormViewRepository.save(medicalFormView);
    }

}

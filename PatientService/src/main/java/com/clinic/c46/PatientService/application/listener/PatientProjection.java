package com.clinic.c46.PatientService.application.listener;


import com.clinic.c46.CommonService.event.patient.PatientCreatedEvent;
import com.clinic.c46.PatientService.application.repository.PatientViewRepository;
import com.clinic.c46.PatientService.domain.event.PatientDeletedEvent;
import com.clinic.c46.PatientService.domain.view.PatientView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientProjection {

    private final PatientViewRepository repository;


    @EventHandler
    public void on(PatientCreatedEvent event) {
        log.debug("Handling PatientCreatedEvent: {}", event);

        PatientView view = PatientView.builder()
                .id(event.patientId())
                .name(event.name())
                .email(event.email())
                .phone(event.phone())
                .build();

        view.create();
        repository.save(view);
    }

    @EventHandler
    public void on(PatientDeletedEvent event) {
        log.debug("Handling PatientDeletedEvent: {}", event);
        repository.findById(event.patientId())
                .ifPresent(view -> {
                    view.delete();
                    repository.save(view);
                });
    }
}

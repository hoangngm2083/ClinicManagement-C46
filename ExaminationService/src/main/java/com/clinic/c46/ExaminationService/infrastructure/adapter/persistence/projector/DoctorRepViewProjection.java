package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.event.staff.DoctorCreatedEvent;
import com.clinic.c46.CommonService.event.staff.DoctorDeletedEvent;
import com.clinic.c46.CommonService.event.staff.DoctorUpdatedEvent;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.DoctorRepViewRepository;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.view.DoctorRepView;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorRepViewProjection {

    private final DoctorRepViewRepository repository;

    @EventHandler
    public void on(DoctorCreatedEvent event) {

        if (repository.existsById(event.doctorId())) return;


        DoctorRepView view = DoctorRepView.builder()
                .id(event.doctorId())
                .name(event.doctorName())
                .eSignature(event.eSignature())
                .build();
        view.markCreated();

        repository.save(view);
    }

    @EventHandler
    public void on(DoctorUpdatedEvent event) {
        Optional<DoctorRepView> existingView = repository.findById(event.doctorId());

        if (existingView.isEmpty()) {
            DoctorRepView newView = DoctorRepView.builder()
                    .id(event.doctorId())
                    .name(event.doctorName())
                    .eSignature(event.eSignature())
                    .build();

            newView.markCreated();
            repository.save(newView);
            return;
        }

        DoctorRepView view = existingView.get();
        view.setName(event.doctorName());
        view.setESignature(event.eSignature());
        view.markUpdated();
        repository.save(view);
    }

    @EventHandler
    public void on(DoctorDeletedEvent event) {
        repository.deleteById(event.doctorId());
    }
}

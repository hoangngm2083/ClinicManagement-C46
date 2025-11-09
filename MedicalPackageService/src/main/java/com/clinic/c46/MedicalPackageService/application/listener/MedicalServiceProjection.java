package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import com.clinic.c46.CommonService.exception.TransientDataNotReadyException;
import com.clinic.c46.MedicalPackageService.application.repository.DepartmentViewRepRepository;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.DepartmentViewRep;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalServiceProjection {

    private final MedicalServiceViewRepository medicalServiceViewRepository;
    private final DepartmentViewRepRepository departmentViewRepository;

    // Create service
    @EventHandler
    @Transactional
    public void on(MedicalServiceCreatedEvent ev) {
        // idempotency: skip if exists
        if (medicalServiceViewRepository.existsById(ev.medicalServiceId())) return;

        DepartmentViewRep dep = ev.departmentId() != null ? departmentViewRepository.findById(ev.departmentId())
                .orElse(null) : null;


        if (dep == null) {
            throw new TransientDataNotReadyException("Department not found yet for departmentId=" + ev.departmentId());
        }

        MedicalServiceView svc = MedicalServiceView.builder()
                .id(ev.medicalServiceId())
                .name(ev.name())
                .description(ev.description())
                .departmentId(ev.departmentId())
                .departmentName(dep.getName())
                .build();

        medicalServiceViewRepository.save(svc);
    }

    @EventHandler
    public void on(MedicalServiceInfoUpdatedEvent event) {
        log.debug("Handling MedicalServiceInfoUpdatedEvent for id={}", event.medicalServiceId());


        medicalServiceViewRepository.findById(event.medicalServiceId())
                .ifPresent(view -> {
                    if (event.name() != null) view.setName(event.name());
                    if (event.description() != null) view.setDescription(event.description());

                    if (event.departmentId() != null && !event.departmentId()
                            .equals(view.getDepartmentId())) {

                        DepartmentViewRep dep = departmentViewRepository.findById(event.departmentId())
                                .orElse(null);
                        if (dep == null) {
                            throw new TransientDataNotReadyException(
                                    "[MedicalServiceInfoUpdatedEvent] [DepartmentNotFound] [departmentId=" + event.departmentId() + "]");
                        }
                        view.setDepartmentName(dep.getName());
                    }
                    medicalServiceViewRepository.save(view);
                });
    }
}

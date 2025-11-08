package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.MedicalPackageService.application.port.out.DepartmentViewRepository;
import com.clinic.c46.MedicalPackageService.application.port.out.MedicalServiceViewRepository;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.view.DepartmentView;
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
    private final DepartmentViewRepository departmentViewRepository;

    // Create service
    @EventHandler
    @Transactional
    public void on(MedicalServiceCreatedEvent ev) {
        // idempotency: skip if exists
        if (medicalServiceViewRepository.existsById(ev.medicalServiceId())) return;

        DepartmentView dep = null;
        if (ev.departmentId() != null) {
            dep = departmentViewRepository.findById(ev.departmentId())
                    .orElse(null);
        }

        MedicalServiceView svc = MedicalServiceView.builder()
                .id(ev.medicalServiceId())
                .name(ev.name())
                .description(ev.description())
                .department(dep)
                .build();

        medicalServiceViewRepository.save(svc);
    }

    @EventHandler
    public void on(MedicalServiceInfoUpdatedEvent event) {
        log.info("📥 Handling MedicalServiceInfoUpdatedEvent for id={}", event.medicalServiceId());

        medicalServiceViewRepository.findById(event.medicalServiceId())
                .ifPresent(view -> {
                    if (event.name() != null) view.setName(event.name());
                    if (event.description() != null) view.setDescription(event.description());
                    if (event.departmentId() != null) view.setDepartment(
                            departmentViewRepository.findById(event.departmentId())
                                    .orElse(null));
                    medicalServiceViewRepository.save(view);
                });
    }
}

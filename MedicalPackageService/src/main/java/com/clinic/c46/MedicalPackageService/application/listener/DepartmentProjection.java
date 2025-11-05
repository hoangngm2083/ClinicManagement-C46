package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.event.staff.DepartmentUpdatedEvent;
import com.clinic.c46.MedicalPackageService.application.port.out.DepartmentViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.DepartmentView;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentProjection {
    private final DepartmentViewRepository departmentRepo;

    // Department replication
    @EventHandler
    @Transactional
    public void on(DepartmentUpdatedEvent ev) {
        DepartmentView dep = departmentRepo.findById(ev.departmentId())
                .map(existing -> {
                    existing.setName(ev.name());
                    return existing;
                })
                .orElse(DepartmentView.builder()
                        .id(ev.departmentId())
                        .name(ev.name())
                        .build());
        departmentRepo.save(dep);
    }
}

package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.event.staff.DepartmentCreatedEvent;
import com.clinic.c46.CommonService.event.staff.DepartmentUpdatedEvent;
import com.clinic.c46.MedicalPackageService.application.repository.DepartmentViewRepRepository;
import com.clinic.c46.MedicalPackageService.domain.view.DepartmentViewRep;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentRepProjection {
    private final DepartmentViewRepRepository departmentRepo;


    @EventHandler
    @Transactional
    public void on(DepartmentCreatedEvent ev) {

        departmentRepo.save(DepartmentViewRep.builder()
                .id(ev.departmentId())
                .name(ev.departmentName())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(DepartmentUpdatedEvent ev) {
        DepartmentViewRep dep = departmentRepo.findById(ev.departmentId())
                .map(existing -> {
                    existing.setName(ev.name());
                    return existing;
                })
                .orElse(DepartmentViewRep.builder()
                        .id(ev.departmentId())
                        .name(ev.name())
                        .build());
        departmentRepo.save(dep);
    }
}

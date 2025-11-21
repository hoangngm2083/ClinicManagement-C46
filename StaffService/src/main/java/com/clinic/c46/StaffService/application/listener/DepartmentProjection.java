package com.clinic.c46.StaffService.application.listener;


import com.clinic.c46.CommonService.event.staff.DepartmentCreatedEvent;
import com.clinic.c46.StaffService.application.repository.DepartmentViewRepository;
import com.clinic.c46.StaffService.domain.view.DepartmentView;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentProjection {

    private final DepartmentViewRepository departmentViewRepository;


    @EventHandler
    @Transactional
    public void on(DepartmentCreatedEvent event) {

        DepartmentView departmentView = new DepartmentView(event.departmentId(), event.departmentName(),
                event.description());
        departmentViewRepository.save(departmentView);

    }

    @EventHandler
    @Transactional
    public void on(com.clinic.c46.CommonService.event.staff.DepartmentDeletedEvent event) {
        departmentViewRepository.findById(event.departmentId())
                .ifPresent(view -> {
                    view.handleDelete();
                    departmentViewRepository.save(view);
                });
    }
}

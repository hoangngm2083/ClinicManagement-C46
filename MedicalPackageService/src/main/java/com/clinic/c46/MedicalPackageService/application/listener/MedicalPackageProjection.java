package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.event.staff.DepartmentUpdatedEvent;
import com.clinic.c46.MedicalPackageService.application.port.out.DepartmentViewRepository;
import com.clinic.c46.MedicalPackageService.application.port.out.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.port.out.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.event.MedicalPackageCreatedEvent;
import com.clinic.c46.MedicalPackageService.domain.event.MedicalServiceCreatedEvent;
import com.clinic.c46.MedicalPackageService.domain.view.DepartmentView;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class MedicalPackageProjection {

    private final MedicalPackageViewRepository packageRepo;
    private final MedicalServiceViewRepository serviceRepo;



    // Create package
    @EventHandler
    @Transactional
    public void on(MedicalPackageCreatedEvent ev) {
        if (packageRepo.existsById(ev.medicalPackageId())) return;

        Set<MedicalServiceView> services = Set.copyOf(serviceRepo.findAllById(ev.serviceIds()));

        MedicalPackageView pkg = MedicalPackageView.builder()
                .id(ev.medicalPackageId())
                .name(ev.name())
                .description(ev.description())
                .price(ev.price())
                .medicalServices(services)
                .build();
        packageRepo.save(pkg);
    }

//    // Reset handler to support replay: clear view tables (careful on production)
//    @ResetHandler
//    @Transactional
//    public void onReset() {
//        // clear projection tables on reset so replay builds fresh view
//        // NOTE: in production consider safer strategies (truncate via repository or custom DAO)
//        packageRepo.deleteAllInBatch();
//        serviceRepo.deleteAllInBatch();
//        departmentRepo.deleteAllInBatch();
//    }
}

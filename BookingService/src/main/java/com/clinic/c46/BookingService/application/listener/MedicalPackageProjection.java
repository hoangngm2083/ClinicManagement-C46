package com.clinic.c46.BookingService.application.listener;

import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicalPackageProjection {

    private final MedicalPackageViewRepository medicalPackageRepository;


    @EventHandler
    public void on(MedicalPackageCreatedEvent event) {
        MedicalPackageView view = MedicalPackageView.builder()
                .medicalPackageId(event.medicalPackageId())
                .medicalPackageName(event.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        medicalPackageRepository.save(view);
    }

    @EventHandler
    public void on(MedicalPackageInfoUpdatedEvent event) {
        medicalPackageRepository.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.setMedicalPackageName(event.name());
                    view.setUpdatedAt(LocalDateTime.now());
                    medicalPackageRepository.save(view);
                });
    }

    @EventHandler
    public void on(MedicalPackageDeletedEvent event) {
        medicalPackageRepository.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.setDeletedAt(LocalDateTime.now());
                    medicalPackageRepository.save(view);
                });
    }

}

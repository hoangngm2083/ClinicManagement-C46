package com.clinic.c46.BookingService.application.listener;


import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.event.AppointmentCreatedEvent;
import com.clinic.c46.BookingService.domain.event.AppointmentStateUpdatedEvent;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentProjection {

    private final AppointmentViewRepository appointmentRepository;
    private final SlotViewRepository slotViewRepository;
    private final MedicalPackageViewRepository medicalPackageViewRepository;
    private final QueryGateway queryGateway;

    @EventHandler
    public void on(AppointmentCreatedEvent event) {

        log.debug("Processing AppointmentCreatedEvent: {}", event);

        SlotView slotView = slotViewRepository.findById(event.slotId())
                .orElseThrow(() -> new IllegalStateException("Slot not found yet for event: " + event.slotId()));

        MedicalPackageView medicalPackageView = medicalPackageViewRepository.findById(slotView.getMedicalPackageId())
                .orElseThrow(() -> new IllegalStateException(
                        "MedicalPackage not available yet for slot: " + slotView.getMedicalPackageId()));

        PatientDto patientDto;
        try {
            patientDto = queryGateway.query(GetPatientByIdQuery.builder()
                            .patientId(event.patientId())
                            .build(), ResponseTypes.instanceOf(PatientDto.class))
                    .join();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch PatientDto for " + event.patientId(), ex);
        }

        AppointmentView view = AppointmentView.builder()
                .id(event.appointmentId())
                .patientId(event.patientId())
                .patientName(patientDto.name())
                .shift(slotView.getShift())
                .date(slotView.getDate())
                .medicalPackage(medicalPackageView)
                .state(AppointmentState.CREATED.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        appointmentRepository.save(view);

        log.debug("AppointmentView saved for appointmentId={}", event.appointmentId());
    }


    @EventHandler
    public void on(AppointmentStateUpdatedEvent event) {
        appointmentRepository.findById(event.appointmentId())
                .ifPresent(view -> {
                    view.setState(event.newState());
                    view.markUpdated();
                    appointmentRepository.save(view);
                });
    }
}

package com.clinic.c46.BookingService.application.listener;


import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.event.AppointmentCreatedEvent;
import com.clinic.c46.BookingService.domain.event.AppointmentStateUpdatedEvent;
import com.clinic.c46.CommonService.event.patient.PatientCreatedEvent;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.BookingService.infrastructure.adapter.exception.DataNotFoundRetryableException;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.GetPatientOptByIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.math.BigDecimal;
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
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttempts = 5,
               backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void on(AppointmentCreatedEvent event) {

        log.debug("Processing AppointmentCreatedEvent: {}", event);

        SlotView slotView = slotViewRepository.findById(event.slotId())
                .orElseThrow(() -> new IllegalStateException("Slot not found yet for event: " + event.slotId()));

        MedicalPackageView medicalPackageView = medicalPackageViewRepository.findById(slotView.getMedicalPackageId())
                .orElseThrow(() -> new IllegalStateException(
                        "MedicalPackage not available yet for slot: " + slotView.getMedicalPackageId()));

        // Fetch patient with retry mechanism
        PatientDto patientDto = fetchPatientWithRetry(event.patientId());

        // Snapshot price and priceVersion at booking time
        BigDecimal currentPrice = medicalPackageView.getCurrentPrice();
        int currentPriceVersion = medicalPackageView.getCurrentPriceVersion();

        AppointmentView view = AppointmentView.builder()
                .id(event.appointmentId())
                .patientId(event.patientId())
                .patientName(patientDto.name())
                .shift(slotView.getShift())
                .date(slotView.getDate())
                .medicalPackage(medicalPackageView)
                .snapshotPrice(currentPrice)
                .snapshotPriceVersion(currentPriceVersion)
                .state(AppointmentState.CREATED.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        appointmentRepository.save(view);

        log.debug("AppointmentView saved for appointmentId={}", event.appointmentId());
    }

    private PatientDto fetchPatientWithRetry(String patientId) {
        try {
            PatientDto patientDto = queryGateway.query(GetPatientOptByIdQuery.builder()
                            .patientId(patientId)
                            .build(), ResponseTypes.instanceOf(PatientDto.class))
                    .join();

            if (patientDto == null) {
                log.warn("Patient not found: {}, will retry", patientId);
                throw new DataNotFoundRetryableException("Patient not found: " + patientId);
            }

            return patientDto;
        } catch (Exception ex) {
            log.error("Failed to fetch PatientDto for {}", patientId, ex);
            throw new DataNotFoundRetryableException("Failed to fetch PatientDto for " + patientId, ex);
        }
    }

    @Recover
    public void recover(DataNotFoundRetryableException e, AppointmentCreatedEvent event) {
        log.error("Failed to process AppointmentCreatedEvent after retries. Patient not available: {}. " +
                  "Appointment will be created without patient details.", event.patientId(), e);

        // Create appointment view without patient details - will be updated later when patient becomes available
        SlotView slotView = slotViewRepository.findById(event.slotId())
                .orElseThrow(() -> new IllegalStateException("Slot not found yet for event: " + event.slotId()));

        MedicalPackageView medicalPackageView = medicalPackageViewRepository.findById(slotView.getMedicalPackageId())
                .orElseThrow(() -> new IllegalStateException(
                        "MedicalPackage not available yet for slot: " + slotView.getMedicalPackageId()));

        // Snapshot price and priceVersion at booking time
        BigDecimal currentPrice = medicalPackageView.getCurrentPrice();
        int currentPriceVersion = medicalPackageView.getCurrentPriceVersion();

        AppointmentView view = AppointmentView.builder()
                .id(event.appointmentId())
                .patientId(event.patientId())
                .patientName("Patient details pending") // Temporary placeholder
                .shift(slotView.getShift())
                .date(slotView.getDate())
                .medicalPackage(medicalPackageView)
                .snapshotPrice(currentPrice)
                .snapshotPriceVersion(currentPriceVersion)
                .state(AppointmentState.CREATED.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        appointmentRepository.save(view);

        log.warn("AppointmentView saved with placeholder patient details for appointmentId={}. " +
                "Will need to be updated when patient becomes available.", event.appointmentId());
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

    @EventHandler
    public void on(com.clinic.c46.BookingService.domain.event.AppointmentRemindedEvent event) {
        appointmentRepository.findById(event.appointmentId())
                .ifPresent(view -> {
                    view.setReminded(true);
                    view.markUpdated();
                    appointmentRepository.save(view);
                });
    }

    @EventHandler
    public void on(PatientCreatedEvent event) {
        log.debug("Processing PatientCreatedEvent: {}", event.patientId());

        // Update any appointments that were created with placeholder patient names
        var appointmentsWithPlaceholder = appointmentRepository
                .findAllByPatientIdAndPatientName(event.patientId(), "Patient details pending");

        if (!appointmentsWithPlaceholder.isEmpty()) {
            log.info("Updating {} appointments for patient {} with real patient name: {}",
                    appointmentsWithPlaceholder.size(), event.patientId(), event.name());

            appointmentsWithPlaceholder.forEach(appointment -> {
                appointment.setPatientName(event.name());
                appointment.markUpdated();
                appointmentRepository.save(appointment);
            });

            log.debug("Successfully updated appointments for patient: {}", event.patientId());
        }
    }
}

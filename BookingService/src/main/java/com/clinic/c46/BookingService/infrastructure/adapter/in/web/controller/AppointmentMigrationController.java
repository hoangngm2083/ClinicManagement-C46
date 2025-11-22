package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;

import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.GetAllPatientsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/appointment/migration")
@RequiredArgsConstructor
@Slf4j
public class AppointmentMigrationController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final SlotViewRepository slotViewRepository;

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedAppointments() {
        log.info("Starting appointment seeding process");

        try {
            // Step 1: Get patient IDs from PatientService via Query Gateway
            List<String> patientIds = getAllPatientsViaQuery();
            log.info("Retrieved {} patients from PatientService", patientIds.size());

            if (patientIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "No patients found in PatientService"));
            }

            // Step 2: Get all available slots
            List<SlotView> availableSlots = slotViewRepository.findAll();
            log.info("Retrieved {} slots from database", availableSlots.size());

            if (availableSlots.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "No slots found in database"));
            }

            // Step 3: Create appointments
            List<String> createdAppointments = new ArrayList<>();
            List<String> failedAppointments = new ArrayList<>();

            for (int i = 0; i < Math.min(availableSlots.size(), patientIds.size()); i++) {
                try {
                    String appointmentId = UUID.randomUUID()
                            .toString();

                    CreateAppointmentCommand command = CreateAppointmentCommand.builder()
                            .appointmentId(appointmentId)
                            .patientId(patientIds.get(i))
                            .slotId(availableSlots.get(i)
                                    .getSlotId())
                            .build();

                    commandGateway.sendAndWait(command);
                    createdAppointments.add(appointmentId);

                } catch (Exception e) {
                    failedAppointments.add(availableSlots.get(i)
                            .getSlotId());
                }

            }


            return ResponseEntity.ok(
                    Map.of("status", "success", "patientsRetrieved", patientIds.size(), "slotsAvailable",
                            availableSlots.size(), "appointmentsCreated", createdAppointments.size(),
                            "appointmentsFailed", failedAppointments.size(), "createdAppointments", createdAppointments,
                            "failedAppointments", failedAppointments));

        } catch (Exception e) {
            log.error("Appointment seeding failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Retrieve all patients from PatientService via QueryGateway
     */
    @SuppressWarnings("unchecked")
    private List<String> getAllPatientsViaQuery() {


        CompletableFuture<List<PatientDto>> result = queryGateway.query(GetAllPatientsQuery.builder()
                .build(), ResponseTypes.multipleInstancesOf(PatientDto.class));

        List<PatientDto> patients = result.join();
        log.info("Successfully retrieved {} patients via QueryGateway", patients.size());


        return patients.stream()
                .map(PatientDto::patientId)
                .toList();


    }


}

package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;


import com.clinic.c46.BookingService.application.service.BookingService;
import com.clinic.c46.BookingService.domain.command.CancelAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateAppointmentRequest;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.UpdateAppointmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final BookingService bookingService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, String>>> createAppointment(
            @RequestBody CreateAppointmentRequest request) {

        String appointmentId = UUID.randomUUID()
                .toString();

        CreateAppointmentCommand cmd = CreateAppointmentCommand.builder()
                .appointmentId(appointmentId)
                .patientId(request.getPatientId())
                .slotId(request.getSlotId())
                .build();

        return bookingService.createAppointment(cmd)
                .thenApply(r -> ResponseEntity.accepted()
                        .body(Map.of("appointmentId", appointmentId)));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public CompletableFuture<ResponseEntity<Map<String, String>>> cancelAppointment(
            @PathVariable String appointmentId) {

        CancelAppointmentCommand cmd = CancelAppointmentCommand.builder()
                .appointmentId(appointmentId)
                .build();

        return bookingService.cancelAppointment(cmd)
                .thenApply(r -> ResponseEntity.accepted()
                        .body(Map.of("appointmentId", appointmentId)));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getAppointment(@PathVariable String appointmentId) {
        Optional<AppointmentView> viewOpt = bookingService.getAppointmentById(appointmentId);
        return viewOpt.map(view -> ResponseEntity.ok()
                        .body(view))
                .orElseGet(() -> ResponseEntity.notFound()
                        .build());
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<?> updateAppointment(@PathVariable String appointmentId,
            @RequestBody UpdateAppointmentRequest request) {

        Optional<AppointmentView> viewOpt = bookingService.getAppointmentById(appointmentId);

        if (viewOpt.isEmpty()) {
            return ResponseEntity.notFound()
                    .build();
        }

        AppointmentView view = viewOpt.get();
        view.setShift(request.getShift());
        view.setDate(request.getDate());
        view.setPatientName(request.getPatientName());
        view.setPatientId(request.getPatientId());
        if (request.getState() != null) {
            view.setState(request.getState());
        }

        AppointmentView saved = bookingService.saveAppointmentView(view);

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String appointmentId) {
        bookingService.deleteAppointment(appointmentId);
        return ResponseEntity.noContent()
                .build();
    }

}

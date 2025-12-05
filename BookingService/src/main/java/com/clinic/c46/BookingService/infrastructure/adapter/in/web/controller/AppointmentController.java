package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;


import com.clinic.c46.BookingService.application.service.BookingService;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.UpdateAppointmentStateCommand;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.query.SearchAppointmentsQuery;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.AppointmentsPagedResponse;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateAppointmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final BookingService bookingService;
    private final QueryGateway queryGateway;

    @GetMapping
    public CompletableFuture<AppointmentsPagedResponse> getAllAppointments(
            @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "0") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "ASC") String sort,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String state,
            @RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {

        SearchAppointmentsQuery query = SearchAppointmentsQuery.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .keyword(keyword)
                .state(state)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        return queryGateway.query(query, AppointmentsPagedResponse.class)
                .thenApply(response -> ResponseEntity.ok(response)
                        .getBody());
    }

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
                .thenApply(r -> ResponseEntity.created(URI.create("/appointments/" + appointmentId))
                        .body(Map.of("appointmentId", appointmentId)));
    }

    @PatchMapping("/{appointmentId}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> updateAppointmentState(
            @PathVariable String appointmentId, @Valid @RequestBody AppointmentState state) {

        UpdateAppointmentStateCommand cmd = UpdateAppointmentStateCommand.builder()
                .appointmentId(appointmentId)
                .newState(state.name())
                .build();

        return bookingService.updateAppointmentState(cmd)
                .thenApply(r -> ResponseEntity.accepted()
                        .body(Map.of("appointmentId", appointmentId, "newState", cmd.newState())));
    }

    @GetMapping("/{appointmentId}")
    public CompletableFuture<ResponseEntity<?>> getAppointment(@PathVariable String appointmentId) {
        return bookingService.getAppointmentById(appointmentId)
                .thenApply(appointmentOpt -> appointmentOpt.map(appointment -> ResponseEntity.ok()
                                .body((Object) appointment))
                        .orElseGet(() -> ResponseEntity.notFound()
                                .build()));
    }


    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String appointmentId) {
        bookingService.deleteAppointment(appointmentId);
        return ResponseEntity.noContent()
                .build();
    }

}

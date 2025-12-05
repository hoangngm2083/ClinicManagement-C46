package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;

import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.command.UpdateSlotMaxQuantityCommand;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateSlotRequest;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.SlotsPagedResponse;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.UpdateSlotMaxQuantityRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.net.URI;
import java.util.Map;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/slot")
@Slf4j
public class SlotController {
    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;


    @PostMapping
    public ResponseEntity<Map<String, String>> createSlot(@RequestBody CreateSlotRequest createSlotRequest) {
        String slotId = UUID.randomUUID()
                .toString();
        CreateSlotCommand createSlotCommand = CreateSlotCommand.builder()
                .date(createSlotRequest.getDate())
                .shift(createSlotRequest.getShift())
                .medicalPackageId(createSlotRequest.getMedicalPackageId())
                .maxQuantity(createSlotRequest.getMaxQuantity())
                .slotId(slotId)
                .build();
        commandGateway.sendAndWait(createSlotCommand);

        return ResponseEntity.created(URI.create("/booking/slot/" + slotId))
                .body(Map.of("slotId", slotId));
    }

    @GetMapping
    public ResponseEntity<SlotsPagedResponse> getAllSlots(@RequestParam("medicalPackageId") String medicalPackageId,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo) {

        if (dateFrom == null) {
            dateFrom = LocalDate.now();
        }
        if (dateTo == null) {
            dateTo = LocalDate.now()
                    .plusWeeks(2);
        }

        SlotsPagedResponse response = queryGateway.query(GetAllSlotOfPackageQuery.builder()
                        .medicalPackageId(medicalPackageId)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .build(), ResponseTypes.instanceOf(SlotsPagedResponse.class))
                .join();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{slotId}")
    public ResponseEntity<Void> updateSlotMaxQuantity(@PathVariable("slotId") String slotId,
            @RequestBody UpdateSlotMaxQuantityRequest request) {
        UpdateSlotMaxQuantityCommand command = UpdateSlotMaxQuantityCommand.builder()
                .slotId(slotId)
                .maxQuantity(request.getMaxQuantity())
                .build();

        commandGateway.sendAndWait(command);
        return ResponseEntity.ok()
                .build();
    }
}

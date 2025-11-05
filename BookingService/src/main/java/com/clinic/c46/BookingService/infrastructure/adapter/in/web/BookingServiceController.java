package com.clinic.c46.BookingService.infrastructure.adapter.in.web;


import com.clinic.c46.BookingService.application.handler.query.dto.SlotDto;
import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateBookingRequest;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateSlotRequest;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bookings") // polling 5s
@RequiredArgsConstructor
public class BookingServiceController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @PostMapping
    public ResponseEntity<Map<String, String>> booking(@RequestBody CreateBookingRequest createBookingRequest,
            @RequestHeader(value = "Fingerprint", required = true) String fingerprint) {

        String bookingId = UUID.randomUUID()
                .toString();

        LockSlotCommand lockSlotCommand = LockSlotCommand.builder()
                .bookingId(bookingId)
                .fingerprint(fingerprint)
                .slotId(createBookingRequest.getSlotId())
                .build();

        commandGateway.sendAndWait(lockSlotCommand);
        Map<String, String> response = Map.of("bookingId", bookingId);

        return ResponseEntity.accepted()
                .body(response);
    }


    @PostMapping("/slots")
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

        return ResponseEntity.created(URI.create("/slots/" + slotId))
                .body(Map.of("slotId", slotId));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDto>> getAllSlots(@RequestParam("medicalPackageId") String medicalPackageId) {
        List<SlotDto> slots = queryGateway.query(GetAllSlotOfPackageQuery.builder()
                        .medicalPackageId(medicalPackageId)
                        .build(), ResponseTypes.multipleInstancesOf(SlotDto.class))
                .join();

        return ResponseEntity.ok(slots);

    }

}

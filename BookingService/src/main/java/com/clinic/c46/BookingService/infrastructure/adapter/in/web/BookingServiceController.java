package com.clinic.c46.BookingService.infrastructure.adapter.in.web;


import com.clinic.c46.BookingService.application.handler.query.dto.SlotDto;
import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import com.clinic.c46.BookingService.domain.query.GetBookingStatusByIdQuery;
import com.clinic.c46.BookingService.domain.view.BookingStatusView;
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
@RequestMapping("/booking") // polling 5s
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
                .email(createBookingRequest.getEmail())
                .name(createBookingRequest.getName())
                .phone(createBookingRequest.getPhone())
                .build();

        commandGateway.sendAndWait(lockSlotCommand);

        return ResponseEntity.accepted()
                .body(Map.of("bookingId", bookingId));
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, BookingStatusView>> getBookingState(@PathVariable String bookingId) {
        BookingStatusView bookingStatusView = queryGateway.query(GetBookingStatusByIdQuery.builder()
                        .bookingId(bookingId)
                        .build(), ResponseTypes.instanceOf(BookingStatusView.class))
                .join();

        return ResponseEntity.ok()
                .body(Map.of("bookingStatus", bookingStatusView));
    }


    @PostMapping("/slot")
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

    @GetMapping("/slot")
    public ResponseEntity<List<SlotDto>> getAllSlots(@RequestParam("medicalPackageId") String medicalPackageId) {
        List<SlotDto> slots = queryGateway.query(GetAllSlotOfPackageQuery.builder()
                        .medicalPackageId(medicalPackageId)
                        .build(), ResponseTypes.multipleInstancesOf(SlotDto.class))
                .join();

        return ResponseEntity.ok(slots);

    }

}

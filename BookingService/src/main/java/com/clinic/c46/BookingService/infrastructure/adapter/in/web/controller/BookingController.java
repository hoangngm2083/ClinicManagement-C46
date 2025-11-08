package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;


import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.query.GetBookingStatusByIdQuery;
import com.clinic.c46.BookingService.domain.view.BookingStatusView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateBookingRequest;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

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

}

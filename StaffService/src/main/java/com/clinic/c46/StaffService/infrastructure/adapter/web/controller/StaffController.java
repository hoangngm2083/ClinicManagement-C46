package com.clinic.c46.StaffService.infrastructure.adapter.web.controller;


import com.clinic.c46.StaffService.application.dto.CreateStaffRequest;
import com.clinic.c46.StaffService.application.dto.RequestDayOffsRequest;
import com.clinic.c46.StaffService.application.dto.StaffDto;
import com.clinic.c46.StaffService.application.dto.UpdateStaffRequest;
import com.clinic.c46.StaffService.application.service.StaffService;
import com.clinic.c46.StaffService.domain.query.FindStaffByIdQuery;
import com.clinic.c46.StaffService.domain.query.FindStaffScheduleQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Validated
public class StaffController {

    private final StaffService staffService;
    private final QueryGateway queryGateway;


    @PostMapping
    public CompletableFuture<ResponseEntity<String>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return staffService.createStaff(request)
                .thenApply(staffId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(staffId));

    }

    @PutMapping("/{staffId}")
    public CompletableFuture<ResponseEntity<Void>> updateStaff(@PathVariable String staffId,
            @Valid @RequestBody UpdateStaffRequest request) {
        return staffService.updateStaff(staffId, request)
                .thenApply(ResponseEntity::ok);

    }

    @PostMapping("/{staffId}/day-off")
    public CompletableFuture<ResponseEntity<Void>> requestDayOff(@PathVariable String staffId,
            @Valid @RequestBody RequestDayOffsRequest request) {
        return staffService.requestDayOff(staffId, request)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/{staffId}")
    public CompletableFuture<ResponseEntity<Void>> deleteStaff(@PathVariable String staffId) {
        return staffService.deleteStaff(staffId)
                .thenApply(ResponseEntity::ok);
    }


    @GetMapping("/{staffId}")
    public CompletableFuture<ResponseEntity<StaffDto>> getStaffById(@PathVariable String staffId) {
        return queryGateway.query(new FindStaffByIdQuery(staffId), ResponseTypes.instanceOf(StaffDto.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/schedule")
    public CompletableFuture<ResponseEntity<List<StaffDto>>> getStaffSchedule(
            @RequestParam @NotNull(message = "Month is required") @Min(value = 1, message = "Month must be at least 1") @Max(value = 12, message = "Month must be at most 12") int month,

            @RequestParam @NotNull(message = "Year is required") @Min(value = 2000, message = "Year must be from 2000") @Max(value = 2100, message = "Year must be up to 2100") int year) {

        FindStaffScheduleQuery query = new FindStaffScheduleQuery(month, year);

        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(StaffDto.class))
                .thenApply(ResponseEntity::ok);
    }
}

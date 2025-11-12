package com.clinic.c46.StaffService.domain.aggregate;


import com.clinic.c46.StaffService.domain.command.CreateStaffCommand;
import com.clinic.c46.StaffService.domain.command.DeleteStaffCommand;
import com.clinic.c46.StaffService.domain.command.RequestDayOffCommand;
import com.clinic.c46.StaffService.domain.command.UpdateStaffInfoCommand;
import com.clinic.c46.StaffService.domain.enums.Role;
import com.clinic.c46.StaffService.domain.event.DayOffRequestedEvent;
import com.clinic.c46.StaffService.domain.event.StaffCreatedEvent;
import com.clinic.c46.StaffService.domain.event.StaffDeletedEvent;
import com.clinic.c46.StaffService.domain.event.StaffInfoUpdatedEvent;
import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Aggregate
@NoArgsConstructor
@Slf4j
public class StaffAggregate {

    @AggregateIdentifier
    private String staffId;
    private String name;
    private String email;
    private String phone;
    private String description;
    private String image; // Base64
    private Role role;
    private String eSignature;
    private boolean active;
    private Set<DayOff> dayOffs;
    private String departmentId;

    @CommandHandler
    public StaffAggregate(CreateStaffCommand command) {
        if (command.name() == null || command.name()
                .isEmpty()) {
            throw new IllegalArgumentException("Staff name cannot be empty");
        }

        AggregateLifecycle.apply(
                new StaffCreatedEvent(command.staffId(), command.name(), command.email(), command.phone(),
                        command.description(), command.image(), command.role(), command.eSignature(),
                        command.departmentId()));
    }

    @EventSourcingHandler
    public void on(StaffCreatedEvent event) {
        this.staffId = event.staffId();
        this.name = event.name();
        this.email = event.email();
        this.phone = event.phone();
        this.description = event.description();
        this.image = event.image();
        this.role = event.role();
        this.eSignature = event.eSignature();
        this.departmentId = event.departmentId();
        this.active = true;
        this.dayOffs = new HashSet<>();
    }

    @CommandHandler
    public void handle(UpdateStaffInfoCommand command) {
        if (!this.active) {
            throw new IllegalStateException("Cannot update inactive staff");
        }

        AggregateLifecycle.apply(
                new StaffInfoUpdatedEvent(command.staffId(), command.name(), command.phone(), command.description(),
                        command.image(), command.role(), command.eSignature(), command.departmentId()));
    }

    @EventSourcingHandler
    public void on(StaffInfoUpdatedEvent event) {
        this.name = event.name();
        this.phone = event.phone();
        this.description = event.description();
        this.image = event.image();
        this.role = event.role();
        this.eSignature = event.eSignature();
        this.departmentId = event.departmentId();
    }

    @CommandHandler
    public void handle(RequestDayOffCommand command) {
        log.warn("=== Aggregate DAYOFFS command: {}", command.dayOffs());
        log.warn("=== Aggregate DAYOFFS before: {}", dayOffs);

        if (!this.active) {
            throw new IllegalStateException("Inactive staff cannot request day off");
        }

        if (command.dayOffs() == null || command.dayOffs()
                .isEmpty()) {
            throw new IllegalArgumentException("Bad day off command");
        }

        if (command.dayOffs()
                .stream()
                .anyMatch(dayOff -> dayOff.getDate()
                        .isBefore(LocalDate.now()))) {
            throw new IllegalArgumentException("Cannot request day off in the past");
        }

        Set<DayOff> dayOffsToCheck = new HashSet<>(command.dayOffs());

        dayOffsToCheck.retainAll(this.dayOffs);

        if (!dayOffsToCheck.isEmpty()) {
            throw new IllegalArgumentException("Day off already requested for this date and shift: " + dayOffsToCheck);
        }

        log.warn("=== Aggregate DAYOFFS after: {}", dayOffs);


        AggregateLifecycle.apply(new DayOffRequestedEvent(this.staffId, command.dayOffs()));
    }

    @EventSourcingHandler
    public void on(DayOffRequestedEvent event) {
        this.dayOffs.addAll(event.dayOffs());
    }

    @CommandHandler
    public void handle(DeleteStaffCommand command) {
        if (!this.active) {
            throw new IllegalStateException("Staff is already inactive");
        }

        AggregateLifecycle.apply(new StaffDeletedEvent(command.staffId()));
    }

    @EventSourcingHandler
    public void on(StaffDeletedEvent event) {
        this.active = false;
    }
}
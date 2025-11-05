package com.clinic.c46.BookingService.domain.aggregate;


import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.command.ReleaseSlotCommand;
import com.clinic.c46.BookingService.domain.enums.Shift;
import com.clinic.c46.BookingService.domain.event.SlotCreatedEvent;
import com.clinic.c46.BookingService.domain.event.SlotLockedEvent;
import com.clinic.c46.BookingService.domain.event.SlotReleasedEvent;
import com.clinic.c46.BookingService.domain.exception.LockedSlotNotFound;
import com.clinic.c46.BookingService.domain.exception.SlotLockConflictException;
import com.clinic.c46.BookingService.domain.exception.SlotUnavailableException;
import com.clinic.c46.BookingService.domain.valueObject.LockedSlot;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Aggregate
public class SlotAggregate {
    @AggregateIdentifier
    private String slotId;
    private LocalDate date;
    private Shift shift;
    private String medicalPackageId;
    private int maxQuantity = 0;
    private List<LockedSlot> lockedSlots;
    private int remainingQuantity = 0;


    // CREATE

    @CommandHandler
    public SlotAggregate(CreateSlotCommand cmd) {

        //Validate The Command
        SlotCreatedEvent event = SlotCreatedEvent.builder()
                .slotId(cmd.slotId())
                .date(cmd.date())
                .medicalPackageId(cmd.medicalPackageId())
                .shift(cmd.shift())
                .maxQuantity(cmd.maxQuantity())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(SlotCreatedEvent event) {
        this.slotId = event.slotId();
        this.date = event.date();
        this.medicalPackageId = event.medicalPackageId();
        this.shift = Shift.fromCode(event.shift());
        this.maxQuantity = event.maxQuantity();
        this.remainingQuantity = this.maxQuantity;
        this.lockedSlots = new ArrayList<>();
    }


    // LOCK
    @CommandHandler
    public void handle(LockSlotCommand cmd) {

        boolean alreadyLocked = this.lockedSlots.stream()
                .anyMatch(ls -> ls.fingerprint()
                        .equals(cmd.fingerprint()));

        if (alreadyLocked) {
            throw new SlotLockConflictException();
        }

        if (this.remainingQuantity <= 0) {
            throw new SlotUnavailableException();
        }

        SlotLockedEvent event = SlotLockedEvent.builder()
                .fingerprint(cmd.fingerprint())
                .slotId(cmd.slotId())
                .bookingId(cmd.bookingId())
                .name(cmd.name())
                .email(cmd.email())
                .phone(cmd.phone())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(SlotLockedEvent event) {
        this.lockedSlots.add(LockedSlot.builder()
                .bookingId(event.bookingId())
                .fingerprint(event.fingerprint())
                .build());

        this.remainingQuantity--;
    }


    // RELEASE
    @CommandHandler
    public void handle(ReleaseSlotCommand cmd) {
        boolean exists = this.lockedSlots.stream()
                .anyMatch(lockedSlot -> lockedSlot.fingerprint()
                        .equals(cmd.fingerprint()));

        if (!exists) {
            throw new LockedSlotNotFound();
        }

        SlotReleasedEvent event = SlotReleasedEvent.builder()
                .slotId(cmd.slotId())
                .fingerprint(cmd.fingerprint())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(SlotReleasedEvent event) {
        this.remainingQuantity++;
        this.lockedSlots.removeIf(lk -> lk.fingerprint()
                .equals(event.fingerprint()));
    }


}

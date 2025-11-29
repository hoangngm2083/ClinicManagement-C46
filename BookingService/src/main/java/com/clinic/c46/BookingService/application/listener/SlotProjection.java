package com.clinic.c46.BookingService.application.listener;


import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.event.LockedSlotReleasedEvent;
import com.clinic.c46.BookingService.domain.event.SlotCreatedEvent;
import com.clinic.c46.BookingService.domain.event.SlotLockedEvent;
import com.clinic.c46.BookingService.domain.event.SlotMaxQuantityUpdatedEvent;
import com.clinic.c46.BookingService.domain.view.SlotView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotProjection {

    private final SlotViewRepository slotRepository;


    @EventHandler
    public void on(SlotCreatedEvent event) {
        SlotView slotView = new SlotView(event.slotId(), event.medicalPackageId(), event.shift(), event.maxQuantity(),
                event.date());

        log.info("===========   Slot Projection: slot {} created    ===========", slotView.getSlotId());
        slotRepository.save(slotView);
    }

    @EventHandler
    public void on(SlotLockedEvent event) {


        SlotView slotView = slotRepository.findById(event.slotId())
                .orElse(null);

        if (slotView == null) throw new IllegalStateException("Slot not found");
        slotView.lock();

        log.info("===========   Slot Projection: slot {} locked    ===========", slotView.getSlotId());
        slotRepository.save(slotView);
    }

    @EventHandler
    public void on(LockedSlotReleasedEvent event) {
        SlotView slotView = slotRepository.findById(event.slotId())
                .orElse(null);

        if (slotView == null) throw new IllegalStateException("Slot not found");
        slotView.lock();

        log.info("===========   Slot Projection: slot {} released ===========", slotView.getSlotId());
        slotRepository.save(slotView);
    }

    @EventHandler
    public void on(SlotMaxQuantityUpdatedEvent event) {
        SlotView slotView = slotRepository.findById(event.slotId())
                .orElseThrow(() -> new IllegalStateException("Slot not found: " + event.slotId()));

        // Update max quantity and adjust remaining quantity
        int difference = event.newMaxQuantity() - event.oldMaxQuantity();
        slotView.updateMaxQuantity(event.newMaxQuantity(), difference);

        log.info("===========   Slot Projection: slot {} max quantity updated from {} to {} ===========",
                slotView.getSlotId(), event.oldMaxQuantity(), event.newMaxQuantity());
        slotRepository.save(slotView);
    }


}

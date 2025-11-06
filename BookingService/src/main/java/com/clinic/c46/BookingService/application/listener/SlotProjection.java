package com.clinic.c46.BookingService.application.listener;


import com.clinic.c46.BookingService.application.port.out.SlotRepository;
import com.clinic.c46.BookingService.domain.event.SlotCreatedEvent;
import com.clinic.c46.BookingService.domain.view.SlotView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlotProjection {

    private final SlotRepository slotRepository;


    @EventHandler
    public void on(SlotCreatedEvent event) {
        SlotView slotView = new SlotView(event.slotId(), event.medicalPackageId(), event.shift(), event.maxQuantity(),
                event.date());
        slotRepository.save(slotView);
    }


}

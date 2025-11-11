package com.clinic.c46.StaffService.application.listener;

import com.clinic.c46.StaffService.application.repository.StaffViewRepository;
import com.clinic.c46.StaffService.domain.event.DayOffRequestedEvent;
import com.clinic.c46.StaffService.domain.event.StaffCreatedEvent;
import com.clinic.c46.StaffService.domain.event.StaffDeletedEvent;
import com.clinic.c46.StaffService.domain.event.StaffInfoUpdatedEvent;
import com.clinic.c46.StaffService.domain.view.StaffView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffProjection {

    private final StaffViewRepository repository;

    @EventHandler
    public void on(StaffCreatedEvent event) {
        log.info("Handling StaffCreatedEvent for staff ID: {}", event.staffId());
        StaffView staffView = new StaffView(event);
        repository.save(staffView);
    }

    @EventHandler
    public void on(StaffInfoUpdatedEvent event) {
        log.info("Handling StaffInfoUpdatedEvent for staff ID: {}", event.staffId());
        repository.findById(event.staffId())
                .ifPresent(staffView -> {
                    staffView.handleUpdate(event);
                    repository.save(staffView);
                });
    }

    @EventHandler
    public void on(DayOffRequestedEvent event) {
        log.info("Handling DayOffRequestedEvent for staff ID: {}", event.staffId());
        repository.findById(event.staffId())
                .ifPresent(staffView -> {
                    staffView.handleDayOffsRequest(event);
                    repository.save(staffView);
                });
    }

    @EventHandler
    public void on(StaffDeletedEvent event) {
        log.info("Handling StaffDeletedEvent for staff ID: {}", event.staffId());
        repository.findById(event.staffId())
                .ifPresent(staffView -> {
                    staffView.handleDelete();
                    repository.save(staffView);
                });
    }
}
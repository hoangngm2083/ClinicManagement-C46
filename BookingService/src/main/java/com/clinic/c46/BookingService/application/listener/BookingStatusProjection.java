package com.clinic.c46.BookingService.application.listener;

import com.clinic.c46.BookingService.application.repository.BookingStatusViewRepository;
import com.clinic.c46.BookingService.domain.event.BookingCompletedEvent;
import com.clinic.c46.BookingService.domain.event.BookingRejectedEvent;
import com.clinic.c46.BookingService.domain.event.SlotLockedEvent;
import com.clinic.c46.BookingService.domain.view.BookingStatusView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingStatusProjection {

    private final BookingStatusViewRepository repository;

    // Bắt đầu: SlotLockedEvent → tạo bản ghi PENDING
    @EventHandler
    public void on(SlotLockedEvent event) {
        BookingStatusView status = new BookingStatusView(event.bookingId());
        repository.save(status);
    }

    @EventHandler
    public void on(BookingCompletedEvent event) {
        repository.findById(event.bookingId())
                .ifPresent(booking -> {
                    booking.approve(event.appointmentId());
                    repository.save(booking);
                });
    }

    @EventHandler
    public void on(BookingRejectedEvent event) {

        log.error("[ Booking Rejected Projection] for [bookingId]: {},  [state]: {}", event.bookingId(),
                event.reason());

        handleRejection(event.bookingId(), event.reason());


    }


    private void handleRejection(String bookingId, String message) {
        repository.findById(bookingId)
                .ifPresent(booking -> {
                    booking.reject(message);
                    repository.save(booking);
                });
    }

}

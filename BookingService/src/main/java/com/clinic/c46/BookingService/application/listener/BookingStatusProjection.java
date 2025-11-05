package com.clinic.c46.BookingService.application.listener;

import com.clinic.c46.BookingService.application.port.out.BookingStatusViewRepository;
import com.clinic.c46.BookingService.application.saga.event.BookingCompletedEvent;
import com.clinic.c46.BookingService.application.saga.event.BookingRejectedEvent;
import com.clinic.c46.BookingService.domain.event.SlotLockedEvent;
import com.clinic.c46.BookingService.domain.view.BookingStatusView;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
                    booking.approve("Đặt lịch thành công!");
                    repository.save(booking);
                });
    }

    @EventHandler
    public void on(BookingRejectedEvent event) {
        handleRejection(event.bookingId(), event.reason());
    }


    private void handleRejection(String bookingId, String message) {
        repository.findById(bookingId)
                .ifPresent(booking -> {
                    booking.reject();
                    repository.save(booking);
                });
    }

}

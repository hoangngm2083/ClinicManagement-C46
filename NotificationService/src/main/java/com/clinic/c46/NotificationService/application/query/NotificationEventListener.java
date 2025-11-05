package com.clinic.c46.NotificationService.application.query;

import com.clinic.c46.NotificationService.application.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


// Projection
@Service
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationRepository repo;
/*

    @EventHandler  // <-- KHÁC @SagaEventHandler
    public void on(CustomerVerifiedEvent event) {
        BookingView view = repo.findById(event.appointmentId());
        view.setPatientVerified(true);
        view.setStatus("VERIFIED");
        repo.save(view);
    }*/
}

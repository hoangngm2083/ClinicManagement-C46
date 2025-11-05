package com.clinic.c46.BookingService.application.port.out;

import com.clinic.c46.BookingService.domain.view.BookingStatusView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusViewRepository extends JpaRepository<BookingStatusView, String> {
}

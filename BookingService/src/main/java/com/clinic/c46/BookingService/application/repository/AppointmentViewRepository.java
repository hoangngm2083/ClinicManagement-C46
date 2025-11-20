package com.clinic.c46.BookingService.application.repository;

import com.clinic.c46.BookingService.domain.view.AppointmentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppointmentViewRepository extends JpaRepository<AppointmentView, String>, JpaSpecificationExecutor<AppointmentView> {
}

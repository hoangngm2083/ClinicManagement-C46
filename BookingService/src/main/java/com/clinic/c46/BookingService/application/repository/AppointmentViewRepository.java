package com.clinic.c46.BookingService.application.repository;

import com.clinic.c46.BookingService.domain.view.AppointmentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentViewRepository extends JpaRepository<AppointmentView, String>, JpaSpecificationExecutor<AppointmentView> {
    List<AppointmentView> findByDateAndState(LocalDate date, String state);
    List<AppointmentView> findByDateAndStateAndIsRemindedFalse(LocalDate date, String state);
    List<AppointmentView> findAllByPatientIdAndPatientName(String patientId, String patientName);
}

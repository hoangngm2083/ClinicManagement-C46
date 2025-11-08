package com.clinic.c46.BookingService.application.repository;

import com.clinic.c46.BookingService.domain.view.SlotView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotViewRepository extends JpaRepository<SlotView, String> {
//    List<SlotEntity> findAllByDateAndShiftAndMedicalPackageId(LocalDate date, Shift shift, String medicalPackageId);
//    List<SlotEntity> findAllByDateAndShift(LocalDate date, Shift shift);
//    List<SlotEntity> findAllByDate(LocalDate date);
    List<SlotView> findAllByMedicalPackageId(String medicalPackageId);
}


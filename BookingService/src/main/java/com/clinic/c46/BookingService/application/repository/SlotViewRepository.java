package com.clinic.c46.BookingService.application.repository;

import com.clinic.c46.BookingService.domain.view.SlotView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SlotViewRepository extends JpaRepository<SlotView, String> {
    //    List<SlotEntity> findAllByDateAndShiftAndMedicalPackageId(LocalDate date, Shift shift, String medicalPackageId);
//    List<SlotEntity> findAllByDateAndShift(LocalDate date, Shift shift);
//    List<SlotEntity> findAllByDate(LocalDate date);
    List<SlotView> findAllByMedicalPackageId(String medicalPackageId);

    Page<SlotView> findAllByMedicalPackageId(String medicalPackageId, Pageable pageable);

    Page<SlotView> findAllByMedicalPackageIdAndDateBetween(String medicalPackageId, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    
    boolean existsByDateAndShiftAndMedicalPackageId(LocalDate date, int shift, String medicalPackageId);

}


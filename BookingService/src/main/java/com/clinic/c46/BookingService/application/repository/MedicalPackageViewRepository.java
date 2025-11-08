package com.clinic.c46.BookingService.application.repository;

import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalPackageViewRepository extends JpaRepository<MedicalPackageView, String> {
}

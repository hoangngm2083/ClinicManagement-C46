package com.clinic.c46.MedicalPackageService.application.port.out;

import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalServiceViewRepository extends JpaRepository<MedicalServiceView, String> {
}

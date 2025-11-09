package com.clinic.c46.MedicalPackageService.application.repository;

import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicalServiceViewRepository extends JpaRepository<MedicalServiceView, String>, JpaSpecificationExecutor<MedicalServiceView> {

}

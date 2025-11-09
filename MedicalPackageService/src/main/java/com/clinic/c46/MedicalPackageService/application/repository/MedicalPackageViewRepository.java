package com.clinic.c46.MedicalPackageService.application.repository;

import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MedicalPackageViewRepository extends JpaRepository<MedicalPackageView, String>, JpaSpecificationExecutor<MedicalPackageView> {
    // useful when we want package + services in one query
    @EntityGraph(attributePaths = "medicalServices")
    Optional<MedicalPackageView> findWithServicesById(String id);
}

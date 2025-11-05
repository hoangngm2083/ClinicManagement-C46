package com.clinic.c46.MedicalPackageService.application.port.out;

import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalPackageViewRepository extends JpaRepository<MedicalPackageView, String> {
    // useful when we want package + services in one query
    @EntityGraph(attributePaths = "medicalServices")
    Optional<MedicalPackageView> findWithServicesById(String id);
}

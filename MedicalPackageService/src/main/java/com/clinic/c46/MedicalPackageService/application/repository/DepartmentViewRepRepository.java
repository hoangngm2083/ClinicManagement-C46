package com.clinic.c46.MedicalPackageService.application.repository;

import com.clinic.c46.MedicalPackageService.domain.view.DepartmentViewRep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentViewRepRepository extends JpaRepository<DepartmentViewRep, String> {
}

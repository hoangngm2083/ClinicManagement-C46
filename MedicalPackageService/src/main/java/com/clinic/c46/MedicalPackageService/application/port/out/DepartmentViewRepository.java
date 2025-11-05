package com.clinic.c46.MedicalPackageService.application.port.out;

import com.clinic.c46.MedicalPackageService.domain.view.DepartmentView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentViewRepository extends JpaRepository<DepartmentView, String> {
}

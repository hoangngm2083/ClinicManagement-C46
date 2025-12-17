package com.clinic.c46.MedicalPackageService.application.repository;

import com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkImportStatusRepository extends JpaRepository<BulkImportStatusView, String> {
}

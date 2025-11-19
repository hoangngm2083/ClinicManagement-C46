package com.clinic.c46.ExaminationService.application.repository;

import com.clinic.c46.ExaminationService.domain.view.DoctorRepView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepViewRepository extends JpaRepository<DoctorRepView, String> {
}


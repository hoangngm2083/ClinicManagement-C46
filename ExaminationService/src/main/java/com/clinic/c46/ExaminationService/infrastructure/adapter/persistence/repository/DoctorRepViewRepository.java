package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.DoctorRepView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepViewRepository extends JpaRepository<DoctorRepView, String> {
}


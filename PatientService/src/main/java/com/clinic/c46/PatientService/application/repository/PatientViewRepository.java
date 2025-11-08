package com.clinic.c46.PatientService.application.repository;


import com.clinic.c46.PatientService.domain.view.PatientView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientViewRepository extends JpaRepository<PatientView, String> {
}

package com.clinic.c46.ExaminationService.application.repository;

import com.clinic.c46.ExaminationService.domain.view.ExamView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamViewRepository extends JpaRepository<ExamView, String>, JpaSpecificationExecutor<ExamView> {
}
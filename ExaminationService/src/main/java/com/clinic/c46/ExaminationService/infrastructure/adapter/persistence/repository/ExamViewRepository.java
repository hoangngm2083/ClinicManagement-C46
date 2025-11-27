package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ExamView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamViewRepository extends JpaRepository<ExamView, String>, JpaSpecificationExecutor<ExamView> {

    @Query("SELECT DISTINCT e FROM ExamView e LEFT JOIN FETCH e.results WHERE e.id = :examId")
    Optional<ExamView> findByIdWithResults(@Param("examId") String examId);
}
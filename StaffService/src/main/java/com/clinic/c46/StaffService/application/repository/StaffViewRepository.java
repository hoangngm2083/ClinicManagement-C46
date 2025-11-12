package com.clinic.c46.StaffService.application.repository;

import com.clinic.c46.StaffService.domain.view.StaffView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffViewRepository extends JpaRepository<StaffView, String>, JpaSpecificationExecutor<StaffView> {

    @Query("""
                SELECT DISTINCT s
                FROM StaffView s
                JOIN s.dayOffs d
                WHERE d.date BETWEEN :startDate AND :endDate
            """)
    List<StaffView> findStaffWithDayOffsBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEmail(String email);
}

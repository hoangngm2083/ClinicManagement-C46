package com.clinic.c46.StaffService.application.repository;

import com.clinic.c46.StaffService.domain.view.StaffView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffViewRepository extends JpaRepository<StaffView, String> {

    @Query("SELECT DISTINCT s FROM StaffView s JOIN s.dayOffs d WHERE FUNCTION('MONTH', d.date) = :month AND FUNCTION('YEAR', d.date) = :year")
    List<StaffView> findStaffWithDayOffsInMonth(@Param("month") int month, @Param("year") int year);

    boolean existsByEmail(String email);
}

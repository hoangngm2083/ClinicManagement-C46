package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.PackageRepView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PackageRepViewRepository extends JpaRepository<PackageRepView, String> {
    long countByIdIn(Set<String> ids);

    @Query("SELECT p.id FROM PackageRepView p WHERE p.id IN :ids")
    Set<String> findIdByPackageIdIn(@Param("ids") Set<String> ids);



}

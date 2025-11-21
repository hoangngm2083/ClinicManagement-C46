package com.clinic.c46.StaffService.application.repository;

import com.clinic.c46.StaffService.domain.view.DepartmentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentViewRepository extends JpaRepository<DepartmentView, String>, JpaSpecificationExecutor<DepartmentView> {


}

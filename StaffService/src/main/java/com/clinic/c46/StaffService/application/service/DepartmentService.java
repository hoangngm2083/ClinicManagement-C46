package com.clinic.c46.StaffService.application.service;

import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import com.clinic.c46.StaffService.domain.command.DeleteDepartmentCommand;

public interface DepartmentService {

    void create(CreateDepartmentCommand cmd);

    void delete(DeleteDepartmentCommand cmd);
}

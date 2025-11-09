package com.clinic.c46.StaffService.application.service;

import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;

public interface DepartmentService {

    void create(CreateDepartmentCommand cmd);
}

package com.clinic.c46.StaffService.application.service;


import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final CommandGateway commandGateway;

    @Override
    public void create(CreateDepartmentCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }
}

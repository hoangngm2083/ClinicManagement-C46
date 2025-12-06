package com.clinic.c46.StaffService.application.service;


import com.clinic.c46.StaffService.application.dto.CreateStaffRequest;
import com.clinic.c46.StaffService.application.dto.RequestDayOffsRequest;
import com.clinic.c46.StaffService.application.dto.UpdateStaffRequest;
import com.clinic.c46.StaffService.application.repository.StaffViewRepository;
import com.clinic.c46.StaffService.domain.command.CreateStaffCommand;
import com.clinic.c46.StaffService.domain.command.DeleteStaffCommand;
import com.clinic.c46.StaffService.domain.command.RequestDayOffCommand;
import com.clinic.c46.StaffService.domain.command.UpdateStaffInfoCommand;
import com.clinic.c46.StaffService.domain.enums.Role;
import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final CommandGateway commandGateway;
    private final StaffViewRepository staffViewRepository;

    @Override
    public CompletableFuture<String> createStaff(CreateStaffRequest request) {
        // Bổ sung check department

        if (staffViewRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String staffId = UUID.randomUUID()
                .toString();

        CreateStaffCommand command = new CreateStaffCommand(staffId, request.name(), request.email(), request.phone(),
                request.description(), request.image(), Role.findByCode(request.role()), request.eSignature(),
                request.departmentId(), request.accountName(), request.password());

        return commandGateway.send(command)
                .thenApply(result -> staffId);
    }

    @Override
    public CompletableFuture<Void> updateStaff(String staffId, UpdateStaffRequest request) {
        // Validate...
        UpdateStaffInfoCommand command = new UpdateStaffInfoCommand(staffId, request.name(), request.phone(),
                request.description(), request.image(), Role.findByCode(request.role()), request.eSignature(),
                request.departmentId());

        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<Void> requestDayOff(String staffId, RequestDayOffsRequest request) {

        if (!staffViewRepository.existsById(staffId)) {
            throw new EntityNotFoundException("Staff id [" + staffId + "] not found");
        }

        RequestDayOffCommand command = new RequestDayOffCommand(staffId, Set.copyOf(request.dayOffs()
                .stream()
                .map(req -> new DayOff(req.date(), req.shift(), req.reason()))
                .collect(Collectors.toSet())));
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<Void> deleteStaff(String staffId) {
        DeleteStaffCommand command = new DeleteStaffCommand(staffId);
        return commandGateway.send(command);
    }
}

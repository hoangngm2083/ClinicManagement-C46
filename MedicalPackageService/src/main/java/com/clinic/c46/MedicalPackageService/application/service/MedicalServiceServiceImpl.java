package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalServiceServiceImpl implements MedicalServiceService {
    private final CommandGateway commandGateway;

    @Override
    public void create(CreateMedicalServiceCommand cmd) {
        log.debug("=== CreateMedicalServiceCommand: {}", cmd);
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void update(UpdateMedicalServiceInfoCommand cmd) {
        log.debug("=== UpdateMedicalServiceInfoCommand: {}", cmd);
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void delete(DeleteMedicalServiceCommand cmd) {
        log.debug("=== DeleteMedicalServiceCommand: {}", cmd);
        commandGateway.sendAndWait(cmd);
    }
}

package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalPackageServiceImpl implements MedicalPackageService {

    private final CommandGateway commandGateway;

    @Override
    public void createPackage(CreateMedicalPackageCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void updatePrice(UpdateMedicalPackagePriceCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void updateInfo(UpdateMedicalPackageInfoCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }

}

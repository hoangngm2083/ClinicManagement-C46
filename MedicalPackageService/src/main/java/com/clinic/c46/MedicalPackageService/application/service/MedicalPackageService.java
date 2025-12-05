package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;

public interface MedicalPackageService {
    void createPackage(CreateMedicalPackageCommand cmd);

    void updatePrice(UpdateMedicalPackagePriceCommand cmd);

    void updateInfo(UpdateMedicalPackageInfoCommand cmd);

    void delete(DeleteMedicalPackageCommand cmd);
}

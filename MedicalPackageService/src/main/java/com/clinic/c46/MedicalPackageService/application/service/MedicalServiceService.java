package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;

public interface MedicalServiceService {

    void create(CreateMedicalServiceCommand cmd);

    void update(UpdateMedicalServiceInfoCommand cmd);

    void delete(DeleteMedicalServiceCommand cmd);
}

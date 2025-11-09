package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;

public interface MedicalServiceService {

    void create(CreateMedicalServiceCommand cmd);
}

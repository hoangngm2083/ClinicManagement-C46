package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.MedicalPackageService.application.port.out.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery;
import com.clinic.c46.MedicalPackageService.domain.query.GetMedicalServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalServiceQueryHandler {

    private final MedicalServiceViewRepository repository;

    @QueryHandler
    public List<MedicalServiceView> handle(GetAllMedicalServicesQuery query) {
        return repository.findAll();
    }

    @QueryHandler
    public MedicalServiceView handle(GetMedicalServiceByIdQuery query) {
        return repository.findById(query.medicalServiceId())
                .orElse(null);
    }
}

package com.clinic.c46.PatientService.application.handler.query;


import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.ExistsPatientByIdQuery;
import com.clinic.c46.CommonService.query.patient.GetAllPatientsQuery;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.PatientService.application.repository.PatientViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientQueryHandler {
    private final PatientViewRepository repository;

    @QueryHandler
    public PatientDto handle(GetPatientByIdQuery query) {
        return repository.findById(query.patientId())
                .map(view -> PatientDto.builder()
                        .patientId(view.getId())
                        .name(view.getName())
                        .email(view.getEmail())
                        .name(view.getName())
                        .build())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
    }

    @QueryHandler
    public List<PatientDto> handle(GetAllPatientsQuery query) {
        return repository.findAll()
                .stream()
                .map(view -> PatientDto.builder()
                        .name(view.getName())
                        .phone(view.getPhone())
                        .patientId(view.getId())
                        .email(view.getEmail())
                        .build())
                .toList();
    }

    @QueryHandler
    public boolean handle(ExistsPatientByIdQuery query) {
        return repository.existsById(query.patientId());
    }


}

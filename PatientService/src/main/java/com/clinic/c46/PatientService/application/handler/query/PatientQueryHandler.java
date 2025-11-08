package com.clinic.c46.PatientService.application.handler.query;


import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.FindPatientByIdQuery;
import com.clinic.c46.CommonService.query.patient.GetAllPatientsQuery;
import com.clinic.c46.PatientService.application.repository.PatientViewRepository;
import com.clinic.c46.PatientService.domain.view.PatientView;
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
    public PatientDto handle(FindPatientByIdQuery query) {
        log.debug("Handling GetPatientByIdQuery: {}", query);
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
    public List<PatientView> handle(GetAllPatientsQuery query) {
        log.debug("Handling GetAllPatientsQuery");
        return repository.findAll();
    }

}

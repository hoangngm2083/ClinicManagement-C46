package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.ExaminationDto;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetMedicalFormByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetMedicalFormDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.MedicalFormMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.MedicalFormView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.MedicalFormViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class MedicalFormQueryHandler {

    private final MedicalFormViewRepository medicalFormViewRepository;
    private final MedicalFormMapper medicalFormMapper;
    private final QueryGateway queryGateway;


    @QueryHandler
    public Optional<MedicalFormDto> handle(GetMedicalFormByIdQuery query) {
        return medicalFormViewRepository.findById(query.medicalFormId())
                .map(medicalFormMapper::toDto);
    }

    @QueryHandler
    public CompletableFuture<Optional<MedicalFormDetailsDto>> handle(GetMedicalFormDetailsByIdQuery query) {

        Optional<MedicalFormView> viewOptional = medicalFormViewRepository.findById(query.medicalFormId());

        if (viewOptional.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        MedicalFormView view = viewOptional.get();

        GetPatientByIdQuery getPatientByIdQuery = GetPatientByIdQuery.builder()
                .patientId(view.getPatientId())
                .build();

        GetExaminationByIdQuery getExaminationByIdQuery = GetExaminationByIdQuery.builder()
                .examinationId(view.getExaminationId())
                .build();

        CompletableFuture<PatientDto> patientFuture = queryGateway.query(getPatientByIdQuery,
                        ResponseTypes.instanceOf(PatientDto.class))
                .handle((patientDto, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to retrieve Patient data: {}", view.getId(), throwable);
                        return null;
                    }
                    return patientDto;
                });

        CompletableFuture<ExaminationDto> examinationFuture = queryGateway.query(getExaminationByIdQuery,
                        ResponseTypes.instanceOf(ExaminationDto.class))

                .handle((examinationDto, throwable) -> {
                    if (throwable != null) {

                        log.warn("Failed to retrieve Examination data: {}", view.getId(), throwable);
                        return null;
                    }
                    return examinationDto;
                });


        return patientFuture.thenCombine(examinationFuture, (patient, examination) -> Optional.of(
                        MedicalFormDetailsDto.builder()
                                .id(view.getId())
                                .medicalFormStatus(view.getMedicalFormStatus())
                                .patient(Optional.ofNullable(patient))
                                .examination(Optional.ofNullable(examination))
                                .build()))
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Critical error during DTO combination for form: {}", view.getId(), throwable);
                        return Optional.empty();
                    }
                    return result;
                });
    }
}

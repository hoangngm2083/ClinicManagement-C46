package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormWithExamDetailsDto;
import com.clinic.c46.CommonService.dto.MedicalFormDto;
import com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetAllMedicalFormsQuery;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
        public List<MedicalFormDto> handle(GetAllMedicalFormsQuery query) {
                log.info("[MedicalFormQueryHandler.handle] Fetching all medical forms");
                return medicalFormViewRepository.findAll().stream()
                                .map(medicalFormMapper::toDto)
                                .collect(Collectors.toList());
        }

        @QueryHandler
        public CompletableFuture<Optional<MedicalFormWithExamDetailsDto>> handle(GetMedicalFormDetailsByIdQuery query) {
                log.info("[MedicalFormQueryHandler.handle] START: Getting medical form details for medicalFormId={}",
                                query.medicalFormId());

                Optional<MedicalFormView> viewOptional = medicalFormViewRepository.findById(query.medicalFormId());

                if (viewOptional.isEmpty()) {
                        log.warn("[MedicalFormQueryHandler.handle] Medical form view NOT found in database for medicalFormId={}",
                                        query.medicalFormId());
                        return CompletableFuture.completedFuture(Optional.empty());
                }

                MedicalFormView view = viewOptional.get();
                log.info(
                                "[MedicalFormQueryHandler.handle] Medical form view found: id={}, patientId={}, examinationId={}, status={}",
                                view.getId(), view.getPatientId(), view.getExaminationId(),
                                view.getMedicalFormStatus());

                GetPatientByIdQuery getPatientByIdQuery = GetPatientByIdQuery.builder()
                                .patientId(view.getPatientId())
                                .build();

                GetExaminationByIdQuery getExaminationByIdQuery = GetExaminationByIdQuery.builder()
                                .examinationId(view.getExaminationId())
                                .build();

                log.debug("[MedicalFormQueryHandler.handle] Querying Patient: patientId={}", view.getPatientId());
                CompletableFuture<PatientDto> patientFuture = queryGateway.query(getPatientByIdQuery,
                                ResponseTypes.instanceOf(PatientDto.class))
                                .handle((patientDto, throwable) -> {
                                        if (throwable != null) {
                                                log.warn(
                                                                "[MedicalFormQueryHandler.handle] FAILED to retrieve Patient data for patientId={}, form={}: {}",
                                                                view.getPatientId(), view.getId(),
                                                                throwable.getMessage(), throwable);
                                                return null;
                                        }
                                        if (patientDto == null) {
                                                log.warn(
                                                                "[MedicalFormQueryHandler.handle] Patient is NULL from query gateway for patientId={}, form={}",
                                                                view.getPatientId(), view.getId());
                                                return null;
                                        }
                                        log.info("[MedicalFormQueryHandler.handle] Patient retrieved successfully: patientId={}, name={}",
                                                        patientDto.patientId(), patientDto.name());
                                        return patientDto;
                                });

                log.debug("[MedicalFormQueryHandler.handle] Querying Examination: examinationId={}",
                                view.getExaminationId());
                CompletableFuture<Optional<ExamDetailsDto>> examinationFuture = queryGateway.query(
                                getExaminationByIdQuery,
                                ResponseTypes.optionalInstanceOf(ExamDetailsDto.class));

                return examinationFuture
                                .thenApply((examinationOpt) -> Optional.of(MedicalFormWithExamDetailsDto.builder()
                                                .id(view.getId())
                                                .medicalFormStatus(view.getMedicalFormStatus())
                                                .examination(examinationOpt)
                                                .build()));
        }
}

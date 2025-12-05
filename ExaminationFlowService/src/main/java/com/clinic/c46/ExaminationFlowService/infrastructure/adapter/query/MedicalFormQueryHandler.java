package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.MedicalFormDto;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.examinationFlow.GetAllMedicalFormsQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetMedicalFormDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.MedicalFormMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.MedicalFormView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.MedicalFormViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.jpa.domain.Specification;
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
    private final SpecificationBuilder specificationBuilder;

    @QueryHandler
    public Optional<MedicalFormDto> handle(GetMedicalFormByIdQuery query) {
        return medicalFormViewRepository.findById(query.medicalFormId())
                .map(medicalFormMapper::toDto);
    }

    @QueryHandler
    public List<MedicalFormDto> handle(GetAllMedicalFormsQuery query) {
        log.info("[MedicalFormQueryHandler.handle] Fetching all medical forms");
        Specification<MedicalFormView> spec = specificationBuilder.notDeleted();

        return medicalFormViewRepository.findAll(spec)
                .stream()
                .map(medicalFormMapper::toDto)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public CompletableFuture<Optional<MedicalFormDto>> handle(GetMedicalFormDetailsByIdQuery query) {

        return CompletableFuture.supplyAsync(() -> medicalFormViewRepository.findById(query.medicalFormId()))
                .thenApply(medicalFormViewOpt -> medicalFormViewOpt.map(medicalFormMapper::toDto));
    }
}

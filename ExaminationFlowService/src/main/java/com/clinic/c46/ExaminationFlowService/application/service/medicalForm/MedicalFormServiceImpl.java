package com.clinic.c46.ExaminationFlowService.application.service.medicalForm;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.patient.ExistsPatientByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.ExistsAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.service.medicalForm.dto.CreateMedicalFormDto;
import com.clinic.c46.ExaminationFlowService.domain.command.CreateMedicalFormCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Validated
public class MedicalFormServiceImpl implements MedicalFormService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;


    @Override
    public CompletableFuture<String> createMedicalForm(
            @Valid CreateMedicalFormDto dto) throws ResourceNotFoundException {
        if (!existsPatientById(dto.patientId())) {
            throw new ResourceNotFoundException("[MedicalFormServiceImpl] Patient Not Found: " + dto.patientId());
        }
        if (!existsAllPackageByIds(dto.medicalPackageIds())) {
            throw new ResourceNotFoundException(
                    "[MedicalFormServiceImpl] Medical Package(s) Not Found: " + dto.medicalPackageIds()
                            .toString());
        }

        String medicalFormId = UUID.randomUUID()
                .toString();

        CreateMedicalFormCommand cmd = CreateMedicalFormCommand.builder()
                .patientId(dto.patientId())
                .medicalFormId(medicalFormId)
                .packageIds(dto.medicalPackageIds())
                .invoiceId(UUID.randomUUID()
                        .toString())
                .examinationId(UUID.randomUUID()
                        .toString())
                .build();
        return commandGateway.send(cmd)
                .thenApply(result -> medicalFormId);
    }


    private boolean existsPatientById(String patientId) {
        return queryGateway.query(ExistsPatientByIdQuery.builder()
                        .patientId(patientId)
                        .build(), ResponseTypes.instanceOf(Boolean.class))
                .join();
    }

    private boolean existsAllPackageByIds(Set<String> medicalPackageIds) {
        ExistsAllPackageByIdsQuery existsAllPackageByIdsQuery = ExistsAllPackageByIdsQuery.builder()
                .packageIds(medicalPackageIds)
                .build();
        return queryGateway.query(existsAllPackageByIdsQuery, ResponseTypes.instanceOf(Boolean.class))
                .join();

    }
}

package com.clinic.c46.ExaminationService.application.service.examination;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsServiceByIdQuery;
import com.clinic.c46.CommonService.query.staff.ExistsStaffByIdQuery;
import com.clinic.c46.ExaminationService.application.service.examination.dto.ExamResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExaminationServiceImpl implements ExaminationService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public CompletableFuture<Void> createResult(String staffId, ExamResultDto examResultDto) {
        log.info("[ExaminationService] Creating result for examId: {}, staffId: {}", examResultDto.examId(), staffId);

        // Run all validations in parallel
        CompletableFuture<Void> staffValidation = validateStaffExists(staffId);
        CompletableFuture<Void> examValidation = validateExaminationExists(examResultDto.examId());
        CompletableFuture<Void> serviceValidation = validateServiceExists(examResultDto.serviceId());

        // Wait for all validations to complete, then send command
        return CompletableFuture.allOf(staffValidation, examValidation, serviceValidation)
                .thenCompose(v -> {
                    AddResultCommand cmd = new AddResultCommand(examResultDto.examId(), examResultDto.doctorId(),
                            examResultDto.serviceId(), examResultDto.data());
                    return commandGateway.send(cmd);
                });
    }

    private CompletableFuture<Void> validateStaffExists(String staffId) {
        CompletableFuture<Boolean> existsFeature = queryGateway.query(new ExistsStaffByIdQuery(staffId),
                ResponseTypes.instanceOf(Boolean.class));
        return existsFeature.thenAccept(exists -> {
            if (Boolean.FALSE.equals(exists)) {
                log.warn("[ExaminationService] Staff not found: {}", staffId);
                throw new ResourceNotFoundException("Nhân viên không tồn tại");
            }
        });
    }

    private CompletableFuture<Void> validateExaminationExists(String examId) {
        CompletableFuture<Optional<ExamDetailsDto>> examFeature = queryGateway.query(
                new GetExaminationByIdQuery(examId), ResponseTypes.optionalInstanceOf(ExamDetailsDto.class));
        return examFeature.thenAccept(examOtp -> {
            if (examOtp.isEmpty()) {
                log.warn("[ExaminationService] Examination not found: {}", examId);
                throw new ResourceNotFoundException("Hồ sơ khám");
            }
        });

    }

    private CompletableFuture<Void> validateServiceExists(String serviceId) {
        CompletableFuture<Boolean> existsFeature = queryGateway.query(new ExistsServiceByIdQuery(serviceId),
                ResponseTypes.instanceOf(Boolean.class));
        return existsFeature.thenAccept(exists -> {
            if (Boolean.FALSE.equals(exists)) {
                log.warn("[ExaminationService] Service not found: {}", serviceId);
                throw new ResourceNotFoundException("Dịch vụ không tồn tại");
            }
        });
    }
}

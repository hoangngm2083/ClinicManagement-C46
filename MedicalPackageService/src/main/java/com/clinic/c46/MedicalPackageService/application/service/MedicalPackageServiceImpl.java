package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.exception.TransientDataNotReadyException;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsAllServicesByIdsQuery;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsMedicalPackageByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalPackageServiceImpl implements MedicalPackageService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public void createPackage(CreateMedicalPackageCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void updatePrice(UpdateMedicalPackagePriceCommand cmd) {
        validateMedicalPackageExists(cmd.medicalPackageId());
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void updateInfo(UpdateMedicalPackageInfoCommand cmd) {
        validateMedicalPackageExists(cmd.medicalPackageId());
        validateAllServicesExist(cmd.serviceIds());
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void delete(DeleteMedicalPackageCommand cmd) {
        validateMedicalPackageExists(cmd.medicalPackageId());
        commandGateway.sendAndWait(cmd);
    }

    private void validateMedicalPackageExists(String medicalPackageId) {
        try {
            Boolean exists = queryGateway.query(new ExistsMedicalPackageByIdQuery(medicalPackageId),
                    ResponseTypes.instanceOf(Boolean.class)).get();
            if (Boolean.FALSE.equals(exists)) {
                log.warn("Medical package not found: {}", medicalPackageId);
                throw new ResourceNotFoundException("Gói khám không tồn tại");
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error validating medical package existence: {}", medicalPackageId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi khi kiểm tra gói khám tồn tại", e);
        }
    }

    private void validateAllServicesExist(java.util.Set<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return;
        }
        try {
            Boolean allExist = queryGateway.query(new ExistsAllServicesByIdsQuery(serviceIds),
                    ResponseTypes.instanceOf(Boolean.class)).get();
            if (Boolean.FALSE.equals(allExist)) {
                log.warn("Some medical services not found in set: {}", serviceIds);
                throw new ResourceNotFoundException("Một số dịch vụ y tế không tồn tại");
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error validating services existence: {}", serviceIds, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi khi kiểm tra dịch vụ tồn tại", e);
        }
    }

    // Test method to validate retry mechanism - simulates eventual consistency failure
    private static final AtomicInteger retryCounter = new AtomicInteger(0);

    @Retryable(
        retryFor = {TransientDataNotReadyException.class},
        maxAttemptsExpression = "#{${retry.maxAttempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.maxDelay}/3}",
            maxDelayExpression = "#{${retry.maxDelay}}",
            multiplier = 2.0
        )
    )
    public String testRetryMechanism(boolean shouldFail) {
        int attempt = retryCounter.incrementAndGet();
        log.info("Test retry mechanism - attempt: {}", attempt);

        if (shouldFail && attempt < 3) {
            log.warn("Simulating eventual consistency failure on attempt {}", attempt);
            throw new TransientDataNotReadyException("Test failure: Data not ready yet, attempt: " + attempt);
        }

        retryCounter.set(0); // Reset for next test
        log.info("Test retry mechanism - SUCCESS on attempt {}", attempt);
        return "Retry test successful after " + attempt + " attempts";
    }
}

package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalServiceServiceImpl implements MedicalServiceService {
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public void create(CreateMedicalServiceCommand cmd) {
        log.debug("=== CreateMedicalServiceCommand: {}", cmd);
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void update(UpdateMedicalServiceInfoCommand cmd) {
        log.debug("=== UpdateMedicalServiceInfoCommand: {}", cmd);
        validateServiceExists(cmd.medicalServiceId());
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void delete(DeleteMedicalServiceCommand cmd) {
        log.debug("=== DeleteMedicalServiceCommand: {}", cmd);
        validateServiceExists(cmd.medicalServiceId());
        commandGateway.sendAndWait(cmd);
    }

    private void validateServiceExists(String serviceId) {
        try {
            Boolean exists = queryGateway.query(new ExistsServiceByIdQuery(serviceId),
                    ResponseTypes.instanceOf(Boolean.class)).get();
            if (Boolean.FALSE.equals(exists)) {
                log.warn("Medical service not found: {}", serviceId);
                throw new ResourceNotFoundException("Dịch vụ y tế không tồn tại");
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error validating service existence: {}", serviceId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi khi kiểm tra dịch vụ tồn tại", e);
        }
    }
}

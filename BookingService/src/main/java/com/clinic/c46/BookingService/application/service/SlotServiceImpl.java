package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.command.UpdateSlotMaxQuantityCommand;
import com.clinic.c46.BookingService.domain.query.ExistsMedicalPackageByIdQuery;
import com.clinic.c46.BookingService.domain.query.ExistsSlotByDateShiftPackageQuery;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateSlotRequest;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.UpdateSlotMaxQuantityRequest;
import com.clinic.c46.CommonService.exception.ResourceExistedException;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public CompletableFuture<String> create(CreateSlotRequest request) {
        String slotId = UUID.randomUUID().toString();

        // Check if medical package exists
        ExistsMedicalPackageByIdQuery medicalPackageQuery = ExistsMedicalPackageByIdQuery.builder()
                .medicalPackageId(request.getMedicalPackageId())
                .build();

        return queryGateway.query(medicalPackageQuery, ResponseTypes.instanceOf(Boolean.class))
                .thenCompose(medicalPackageExists -> {
                    if (Boolean.FALSE.equals(medicalPackageExists)) {
                        throw new ResourceNotFoundException("Gói khám");
                    }

                    // Check if slot already exists
                    ExistsSlotByDateShiftPackageQuery existsQuery = ExistsSlotByDateShiftPackageQuery.builder()
                            .date(request.getDate())
                            .shift(request.getShift())
                            .medicalPackageId(request.getMedicalPackageId())
                            .build();

                    return queryGateway.query(existsQuery, ResponseTypes.instanceOf(Boolean.class))
                            .thenApply(slotExists -> {
                                if (Boolean.TRUE.equals(slotExists)) {
                                    throw new ResourceExistedException("Slot");
                                }
                                return slotId;
                            });
                })
                .thenCompose(id -> {
                    CreateSlotCommand command = CreateSlotCommand.builder()
                            .date(request.getDate())
                            .shift(request.getShift())
                            .medicalPackageId(request.getMedicalPackageId())
                            .maxQuantity(request.getMaxQuantity())
                            .slotId(id)
                            .build();
                    return commandGateway.send(command).thenApply(v -> id);
                });
    }

    @Override
    public CompletableFuture<Void> update(String slotId, UpdateSlotMaxQuantityRequest request) {
        UpdateSlotMaxQuantityCommand command = UpdateSlotMaxQuantityCommand.builder()
                .slotId(slotId)
                .maxQuantity(request.getMaxQuantity())
                .build();

        return commandGateway.send(command);
    }
}

package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.staff.ExistsStaffByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDto;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemResponse;
import com.clinic.c46.ExaminationFlowService.application.query.*;
import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.command.ApproveAdditionalServicesCommand;
import com.clinic.c46.ExaminationFlowService.domain.command.TakeNextItemCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final WebSocketNotifier webSocketNotifier;


    @Override
    public void requestGetQueueItem(String doctorId, String queueId) {

        if (isStaffExisted(doctorId)) {
            handleException(doctorId, new ResourceNotFoundException("Mã nhân viên"));
            return;
        }

        Boolean isStaffInProcess = queryGateway.query(new ExistProcessingItemQuery(doctorId), Boolean.class)
                .join();

        if (Boolean.TRUE.equals(isStaffInProcess)) {
            handleException(doctorId, new IllegalStateException("Bạn đang có một hồ sơ khác cần xử lý!"));
            return;
        }

        Optional<String> itemId = queryGateway.query(new GetItemIdOfTopQueueQuery(queueId),
                        ResponseTypes.optionalInstanceOf(String.class))
                .join();

        if (itemId.isEmpty()) {
            handleException(doctorId, new ResourceNotFoundException("Bệnh nhân đang chờ"));
            return;
        }

        commandGateway.send(new TakeNextItemCommand(itemId.get(), doctorId))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        handleException(doctorId, throwable);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> requestAdditionalServices(String doctorId, String queueItemId,
            Set<String> additionalServiceIds) {
        if (isStaffExisted(doctorId)) {
            throw new ResourceNotFoundException("Mã nhân viên");
        }

        Boolean isPackageExisted = queryGateway.query(new ExistsAllPackageByIdsQuery(additionalServiceIds),
                        ResponseTypes.instanceOf(Boolean.class))
                .join();

        if (Boolean.FALSE.equals(isPackageExisted)) {
            throw new ResourceNotFoundException("Các dịch vụ yêu cầu bổ sung");
        }

        Optional<QueueItemDto> queueItemDto = queryGateway.query(new GetQueueItemByIdQuery(queueItemId),
                        ResponseTypes.optionalInstanceOf(QueueItemDto.class))
                .join();

        if (queueItemDto.isEmpty()) {
            throw new ResourceNotFoundException("Hồ sơ khám");
        }

        QueueItemDto queueItem = queueItemDto.get();

        ApproveAdditionalServicesCommand command = ApproveAdditionalServicesCommand.builder()
                .medicalFormId(queueItem.medicalFormId())
                .additionalServiceIds(additionalServiceIds)
                .build();

        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<Void> completeItem(String queueItemId, ExamResultDto examResultDto) {

        // TODO: check queue item, exam, doctor, service existed?

        AddResultCommand cmd = new AddResultCommand(examResultDto.examId(), examResultDto.doctorId(),
                examResultDto.serviceId(), examResultDto.data());

        return commandGateway.send(cmd);
    }

    @Override
    public CompletableFuture<Optional<QueueItemResponse>> getInProgressItem(String staffId) {
        return queryGateway.query(new GetInProgressQueueItemByStaffIdQuery(staffId),
                ResponseTypes.optionalInstanceOf(QueueItemResponse.class));

    }

    private boolean isStaffExisted(String staffId) {
        Boolean isStaffExisted = queryGateway.query(new ExistsStaffByIdQuery(staffId),
                        ResponseTypes.instanceOf(Boolean.class))
                .join();

        return Boolean.FALSE.equals(isStaffExisted);
    }

    private void handleException(String staffId, Throwable throwable) {

        if (throwable != null) {
            Throwable actual = throwable;

            // Bóc CompletionException
            if (actual instanceof CompletionException && actual.getCause() != null) {
                actual = actual.getCause();
            }
            // Bóc CommandExecutionException
            if (actual instanceof CommandExecutionException && actual.getCause() != null) {
                actual = actual.getCause();
            }
            webSocketNotifier.notifyErrorToUser(staffId, actual.getMessage());
        }

    }

}

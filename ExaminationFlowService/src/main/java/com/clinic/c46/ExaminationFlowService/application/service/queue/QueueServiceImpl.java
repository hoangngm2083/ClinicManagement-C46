package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.staff.ExistsStaffByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemResponse;
import com.clinic.c46.ExaminationFlowService.application.query.ExistProcessingItemQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetInProgressQueueItemByStaffIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetItemIdOfTopQueueQuery;
import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.command.TakeNextItemCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
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
    private final EventGateway eventGateway;
    private final WebSocketNotifier webSocketNotifier;

    @Override
    public void dequeue(String queueId) {
        // TODO: Controller phát event TakeQueueItemRequestedEvent -> kích hoạt ExamFlow
        // Saga
        // TODO: ExamFlow Saga gửi command dequeue vào aggregate
        // TODO: Aggregate phát event Taken
        // TODO: Saga gửi command
    }

    @Override
    public void requestGetQueueItem(String doctorId, String queueId) {

        Boolean isStaffExisted = queryGateway.query(new ExistsStaffByIdQuery(doctorId),
                        ResponseTypes.instanceOf(Boolean.class))
                .join();

        if (Boolean.FALSE.equals(isStaffExisted)) {
            handleException(doctorId, new ResourceNotFoundException("Mã nhân viên (" + doctorId + ")"));
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
            handleException(doctorId, new ResourceNotFoundException("Bệnh nhân nào đang chờ"));
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
        return null;
    }

    @Override
    public CompletableFuture<Void> completeItem(String queueItemId, ExamResultDto examResultDto) {

        Boolean isStaffExisted = queryGateway.query(new ExistsStaffByIdQuery(examResultDto.doctorId()),
                        ResponseTypes.instanceOf(Boolean.class))
                .join();

        if (Boolean.FALSE.equals(isStaffExisted)) {
            throw new ResourceNotFoundException("Mã nhân viên (" + examResultDto.doctorId() + ")");
        }

        AddResultCommand cmd = new AddResultCommand(examResultDto.examId(), examResultDto.doctorId(),
                examResultDto.serviceId(), examResultDto.data());


        return commandGateway.send(cmd);
    }


    @Override
    public void getInProgressItem(String staffId) {
        Optional<QueueItemResponse> queueItemResponseOpt = queryGateway.query(
                        new GetInProgressQueueItemByStaffIdQuery(staffId),
                        ResponseTypes.optionalInstanceOf(QueueItemResponse.class))
                .join();

        if (queueItemResponseOpt.isEmpty()) {
            handleException(staffId, new ResourceNotFoundException("Phiếu khám đang xử lý"));
            return;
        }

        QueueItemResponse queueItemResponse = queueItemResponseOpt.get();
        webSocketNotifier.sendToUser(staffId, queueItemResponse);
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

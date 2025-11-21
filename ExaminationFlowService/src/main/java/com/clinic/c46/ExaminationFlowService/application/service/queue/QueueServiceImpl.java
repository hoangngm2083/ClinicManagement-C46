package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final CommandGateway commandGateway;
    private final WebSocketNotifier webSocketNotifier;


    @Override
    public void dequeue(String queueId) {
        // TODO: Controller phát event TakeQueueItemRequestedEvent -> kích hoạt ExamFlow Saga
        // TODO: ExamFlow Saga gửi command dequeue vào aggregate
        // TODO: Aggregate phát event Taken
        // TODO: Saga gửi command
    }

    @Override
    public void requestGetQueueItem(String doctorId) {

    }

    @Override
    public void completeItem(String queueItemId, ExamResultDto examResultDto) {

        //TODO: kiểm tra trạng thái queueItemId có phải là IN_PROGRESS khônng

        AddResultCommand cmd = new AddResultCommand(examResultDto.examId(), examResultDto.doctorId(),
                examResultDto.serviceId(), examResultDto.data());

        commandGateway.send(cmd)
                .whenComplete((cmd1, throwable) -> {
                    if (throwable != null) {
                        handleException(examResultDto.doctorId(), throwable);
                    }
                });
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

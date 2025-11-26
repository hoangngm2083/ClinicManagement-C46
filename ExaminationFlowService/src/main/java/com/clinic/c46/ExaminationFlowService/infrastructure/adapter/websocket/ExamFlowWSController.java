package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket;

import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.service.queue.QueueService;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.RequestAdditionalServicesRequest;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.TakeNextItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

@Controller
@RequiredArgsConstructor
@Validated
@Slf4j
@MessageMapping("/exam-workflow")
public class ExamFlowWSController {
    private final QueryGateway queryGateway;
    private final WebSocketNotifier wSNotifier;
    private final QueueService queueService;

    @MessageMapping("/queue/take-next")
    public void handle(@Payload @Valid TakeNextItemRequest request, Principal principal) {
        String staffId = principal.getName();
        String queueId = request.queueId();
        queueService.requestGetQueueItem(staffId, queueId);
    }

    @MessageMapping("item/request-additional-services")
    public CompletableFuture<Void> handle(@Payload RequestAdditionalServicesRequest request, Principal principal) {

        String staffId = principal.getName();

        return wrapper(staffId, () -> queueService.requestAdditionalServices(staffId, request.queueItemId(),
                request.additionalServiceIds()));
    }

    @MessageMapping("item/in-process")
    public CompletableFuture<Void> handle(Principal principal) {
        String staffId = principal.getName();

        return wrapper(staffId, () -> queueService.getInProgressItem(staffId)
                .thenAccept(result -> {

                    result.ifPresentOrElse((value) -> wSNotifier.sendToUser(staffId, value),
                            () -> wSNotifier.notifyErrorToUser(staffId, "Bạn không có phiếu khám nào đang xử lý."));
                }));

    }

    @MessageMapping("query/queue-size")
    public void handle(@Payload String queueId, Principal principal) {
        // Remove quotes if present (due to JSON.stringify on client side)
        String cleanQueueId = queueId.replace("\"", "");

        log.warn("====== [WS] Received queueId from client: '{}', cleaned: '{}' ======", queueId, cleanQueueId);

        Long qSize = queryGateway.query(new GetQueueSizeQuery(cleanQueueId), ResponseTypes.instanceOf(Long.class))
                .join();

        log.warn("====== [WS] Query returned qSize: {} for queueId: '{}' ======", qSize, cleanQueueId);
        wSNotifier.sendToUser(principal.getName(), WebSocketNotifierImpl.STAFF_SPECIFIC_GET_QUEUE_SIZE_URL, qSize);
    }

    private <T> CompletableFuture<T> wrapper(String staffId, Supplier<CompletableFuture<T>> func) {
        try {
            return func.get()
                    .exceptionally(ex -> {
                        handleException(staffId, ex);
                        return null;
                    });
        } catch (Exception ex) {
            handleException(staffId, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    private void handleException(String staffId, Throwable throwable) {
        if (throwable == null)
            return;

        Throwable actual = throwable;

        while ((actual instanceof CompletionException || actual instanceof CommandExecutionException)
                && actual.getCause() != null) {
            actual = actual.getCause();
        }

        wSNotifier.notifyErrorToUser(staffId, actual.getMessage());
    }

}
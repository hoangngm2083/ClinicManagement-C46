package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket;

import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.service.queue.QueueService;
import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.event.TakeNextItemRequestedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.CompleteItemRequest;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.RequestAdditionalServicesRequest;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.TakeNextItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExamFlowWSController {

    private final CommandGateway commandGateway;
    private final EventGateway eventGateway;
    private final QueryGateway queryGateway;
    private final WebSocketNotifier WSNotifier;
    private final QueueService queueService;

    @MessageMapping("/exam-workflow/queue/take-next")
    public void handle(@Payload @Valid TakeNextItemRequest request, Principal principal) {
        String staffId = principal.getName();
        String queueId = request.queueId();
        eventGateway.publish(new TakeNextItemRequestedEvent(staffId, queueId));
    }

    @MessageMapping("/exam-workflow/item/complete")
    public void handle(@Payload @Valid CompleteItemRequest request, Principal principal) {
        // TODO: nhận payload của người dùng -> gọi queue service (injected)  để queue service thực hiện
        String staffId = principal.getName();
        queueService.completeItem(request.queueItemId(), ExamResultDto.builder()
                .examId(request.examId())
                .doctorId(staffId)
                .serviceId(request.serviceId())
                .data(request.data())
                .build());
    }

    @MessageMapping("/exam-workflow/item/request-additional-services")
    public void handle(@Payload RequestAdditionalServicesRequest request, Principal principal) {
        // TODO: nhận payload của người dùng -> gọi queue service (injected)  để queue service thực hiện
        String staffId = principal.getName();
    }

    @MessageMapping("/exam-workflow/query/queue-size")
    public void handle(@Payload String queueId, Principal principal) {

        log.warn("====== QueueId = {} =======", queueId);
        log.warn("====== Principal = {} =======", principal.getName());
        Long qSize = queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
                .join();

        log.warn("====== Q size = {} =======", qSize);

        WSNotifier.sendToUser(principal.getName(), WebSocketNotifierImpl.STAFF_SPECIFIC_GET_QUEUE_SIZE_URL, qSize);
    }


}
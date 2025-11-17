package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket;

import com.clinic.c46.ExaminationFlowService.application.query.GetQueueSizeQuery;
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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExamFlowWSController {

    private final CommandGateway commandGateway;
    private final EventGateway eventGateway;
    private final QueryGateway queryGateway;
    private final WebSocketNotifier WSNotifier;

    @MessageMapping("/exam-workflow/queue/take-next")
    public void handle(@Payload @Valid TakeNextItemRequest request, Principal principal) {

        log.info("====== TakeNextItemRequest = {} =======", request);
        log.debug("====== Principal = {} =======", principal.getName());
        // TODO: nhận payload của staff -> gọi queue service (injected)  để queue service thực hiện lấy queue item
        String staffId = principal.getName();
        String queueId = request.queueId();
        eventGateway.publish(new TakeNextItemRequestedEvent(staffId, queueId));
    }

    @MessageMapping("/exam-workflow/item/complete")
    public void handle(@Payload @Valid CompleteItemRequest request, Principal principal) {
        // TODO: nhận payload của người dùng -> gọi queue service (injected)  để queue service thực hiện
        String staffId = principal.getName();
    }

    @MessageMapping("/exam-workflow/item/request-additional-services")
    public void handle(@Payload RequestAdditionalServicesRequest request, Principal principal) {
        // TODO: nhận payload của người dùng -> gọi queue service (injected)  để queue service thực hiện
        String staffId = principal.getName();
    }

    @MessageMapping("/exam-workflow/query/queue-size")
    public void handle(@Payload String queueId, SimpMessageHeaderAccessor headerAccessor) {
        String user = Objects.requireNonNull(headerAccessor.getUser())
                .getName();
        Long qSize = queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
                .join();
        WSNotifier.sendToUser(user, qSize);
    }


}
package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket;

import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotifierImpl implements WebSocketNotifier {

    private final String STAFF_SPECIFIC_NOTIFY_ERROR_URL = "/queue/errors";
    private final String STAFF_SPECIFIC_SEND_ITEM_URL = "/queue/exam-workflow/item/details";
    private final String STAFF_SPECIFIC_GET_QUEUE_SIZE_URL = "/queue/query-size-reply";
    private final SimpMessagingTemplate webSocketPusher;
    private final QueryGateway queryGateway;

    private String genPubSubUrl(String queueId) {
        return String.format("/topic/exam-workflow/queue/%s/list", queueId);
    }


    @Override
    public void sendToUser(String userId, Object payload) {

        log.warn("======== WebSocketNotifierImpl : {} ========", userId);

        webSocketPusher.convertAndSendToUser(userId, STAFF_SPECIFIC_SEND_ITEM_URL, payload);
    }

    @Override
    public void notifyErrorToUser(String staffId, String errorMessage) {
        webSocketPusher.convertAndSendToUser(staffId, STAFF_SPECIFIC_NOTIFY_ERROR_URL, errorMessage);
    }

    @Override
    public void broadcast(String queueId, Object payload) {
        webSocketPusher.convertAndSend(genPubSubUrl(queueId), payload);
    }
}

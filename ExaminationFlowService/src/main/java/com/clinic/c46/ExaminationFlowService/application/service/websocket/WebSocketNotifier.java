package com.clinic.c46.ExaminationFlowService.application.service.websocket;

public interface WebSocketNotifier {
    void sendToUser(String userId, Object payload);

    void sendToUser(String userId, String url, Object payload);

    void notifyErrorToUser(String staffId, String errorMessage);

    void broadcast(String queueId, Object payload);

}

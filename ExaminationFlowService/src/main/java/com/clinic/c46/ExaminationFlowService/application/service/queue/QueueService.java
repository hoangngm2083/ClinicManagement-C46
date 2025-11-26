package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemResponse;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface QueueService {

    void requestGetQueueItem(String doctorId, String queueId);

    CompletableFuture<Void> requestAdditionalServices(String doctorId, String queueItemId,
            Set<String> additionalServiceIds);

    CompletableFuture<Optional<QueueItemResponse>> getInProgressItem(String staffId);
}

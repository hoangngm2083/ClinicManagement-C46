package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface QueueService {
    // void schedule(QueueItem queueItem);

    void dequeue(String queueId);

    void requestGetQueueItem(String doctorId, String queueId);

    CompletableFuture<Void> requestAdditionalServices(String doctorId, String queueItemId,
            Set<String> additionalServiceIds);

    CompletableFuture<Void> completeItem(String queueItemId, ExamResultDto examResultDto);

    void getInProgressItem(String staffId);
}

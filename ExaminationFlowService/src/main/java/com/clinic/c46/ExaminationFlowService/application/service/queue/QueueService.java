package com.clinic.c46.ExaminationFlowService.application.service.queue;

public interface QueueService {
//    void schedule(QueueItem queueItem);

    void dequeue(String queueId);
    void requestGetQueueItem(String doctorId);
}

package com.clinic.c46.ExaminationFlowService.application.service.queue;

import com.clinic.c46.ExaminationFlowService.application.service.queue.dto.ExamResultDto;

public interface QueueService {
    // void schedule(QueueItem queueItem);

    void dequeue(String queueId);

    void requestGetQueueItem(String doctorId, String queueId);

    void completeItem(String queueItemId, ExamResultDto examResultDto);

    void getInProgressItem(String staffId);
}

package com.clinic.c46.ExaminationFlowService.application.service.queue;

public class QueueServiceImpl implements QueueService {


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
}

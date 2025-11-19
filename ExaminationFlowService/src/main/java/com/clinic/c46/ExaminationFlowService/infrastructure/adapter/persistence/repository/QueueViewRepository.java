package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import java.util.Optional;

public interface QueueViewRepository {

    void createQueue(String queueId);
    boolean isInProgress(String queueItemId);

    boolean isInQueue(String queueId, String queueItemId);
    boolean isCompleted(String queueId, String queueItemId);
    Long getQueueSize(String queueId);

    /**
     * Atomically take the oldest item from main and move to processing.
     * This is FIFO: we pop from the right (oldest) and push to left of processing.
     */
    void handleTakeNext(String queueId);

    /**
     * Complete an item: remove it from processing list (LREM), and optionally push to history.
     */
    boolean complete(String queueId, String itemId);

    /**
     * Rollback an item: remove from processing and push to HEAD of main queue (leftPush)
     * so it will be next processed (2-way queue requirement).
     */
    boolean rollbackToHead(String queueId, String itemId);

    /**
     * Peek at head (oldest) without removing — useful if you only need to inspect.
     * NOTE: not atomic with takeNext; use takeNext for actual dequeue.
     */
    Optional<String> peekHead(String queueId);

    /**
     * optionally: method to push new item to tail (enqueue)
     */
    void enqueueToTail(String queueId, String itemId);

    /**
     * Delete queue structures (main, history and flags) for a given queueId
     */
    void deleteQueue(String queueId);
}

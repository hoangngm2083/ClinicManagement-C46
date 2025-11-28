package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Getter
@Slf4j
public class RedisQueueViewRepositoryImpl implements QueueViewRepository {

    /**
     * Nếu hàng đợi source hiện đang rỗng, lệnh sẽ chờ trong khoảng thời gian xác
     * định (TIMEOUT_SECONDS).
     * Nếu có item xuất hiện trong thời gian chờ, nó sẽ được lấy đi;
     * Nếu hết thời gian chờ mà không có item nào, lệnh sẽ trả về null.
     *
     */
    private final int TIMEOUT_SECONDS = 0; // Non-blocking

    private final RedisTemplate<String, String> redisTemplate;

    // keys pattern
    private String mainKey(String queueId) {
        return "queue:" + queueId + ":main";
    }

    private String procKey() {
        return "queue:processing";
    }

    private String historyKey(String queueId) {
        return "queue:" + queueId + ":history";
    }

    @Override
    public void createQueue(String queueId) {
        initializeListQueue(mainKey(queueId));
        initializeListQueue(historyKey(queueId));
        initializeListQueue(procKey());

    }

    private void initializeListQueue(String keyPrefix) {
        // Tên Key Flag để kiểm tra xem đã khởi tạo chưa
        String initFlagKey = keyPrefix + ":init_flag";

        // Sử dụng setIfAbsent (tương đương SETNX)
        // Trả về true nếu Key Flag chưa tồn tại và được set thành công
        Boolean isInitialized = redisTemplate.opsForValue()
                .setIfAbsent(initFlagKey, "1");

        if (Boolean.TRUE.equals(isInitialized)) {
            // Key Flag chưa tồn tại, setIfAbsent thành công => Khởi tạo Queue
            log.info("Khởi tạo Redis List: {}", keyPrefix);

            // Logic tạo List rỗng vật lý:
            // 1. LPUSH phần tử placeholder
            redisTemplate.opsForList()
                    .rightPush(keyPrefix, "__INIT_PLACEHOLDER__");
            // 2. LTRIM List về kích thước rỗng (giữ từ index 1 đến 0)
            redisTemplate.opsForList()
                    .trim(keyPrefix, 1, 0);
        } else {
            // Key Flag đã tồn tại => Đã được khởi tạo trước đó, bỏ qua.
            log.info("Redis List đã tồn tại, bỏ qua: {}", keyPrefix);
        }
    }

    @Override
    public boolean isInProgress(String queueItemId) {
        String procKey = procKey();
        List<String> processingItems = redisTemplate.opsForList()
                .range(procKey, 0, -1);
        return processingItems != null && processingItems.contains(queueItemId);
    }

    @Override
    public boolean isInQueue(String queueId, String queueItemId) {
        String queueKey = mainKey(queueId);
        List<String> processingItems = redisTemplate.opsForList()
                .range(queueKey, 0, -1);
        return processingItems != null && processingItems.contains(queueItemId);
    }

    @Override
    public boolean isCompleted(String queueId, String queueItemId) {
        String queueKey = historyKey(queueId);
        List<String> processingItems = redisTemplate.opsForList()
                .range(queueKey, 0, -1);
        return processingItems != null && processingItems.contains(queueItemId);
    }

    @Override
    public Long getQueueSize(String queueId) {
        String queueKey = mainKey(queueId);
        return redisTemplate.opsForList()
                .size(queueKey);
    }

    /**
     * Atomically take the oldest item from main and move to processing.
     * This is FIFO: we pop from the right (oldest) and push to left of processing.
     * timeoutSeconds = 0 -> nonblocking
     */
    public void handleTakeNext(String queueId) {
        String source = mainKey(queueId);
        String dest = procKey();
        /**
         * moved: Biến này sẽ chứa item đã được di chuyển (thường là một chuỗi) nếu thao
         * tác thành công,
         * hoặc chứa null nếu hết thời gian chờ mà không có item nào.
         */
        redisTemplate.opsForList()
                .rightPopAndLeftPush(source, dest, this.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Complete an item: remove it from processing list (LREM), and optionally push
     * to history.
     */
    public void complete(String queueId, String itemId) {
        String proc = procKey();
        Long removed = redisTemplate.opsForList()
                .remove(proc, 1, itemId); // remove first match
        if (removed != null && removed > 0) {
            redisTemplate.opsForList()
                    .leftPush(historyKey(queueId), itemId); // archive
        }
    }

    /**
     * Rollback an item: remove from processing and push to HEAD of main queue
     * (leftPush)
     * so it will be next processed (2-way queue requirement).
     */
    public boolean rollbackToHead(String queueId, String itemId) {
        String proc = procKey();
        Long removed = redisTemplate.opsForList()
                .remove(proc, 1, itemId);
        if (removed != null && removed > 0) {
            // Sử dụng rightPush để nó được xử lý ngay lập tức
            redisTemplate.opsForList()
                    .rightPush(mainKey(queueId), itemId); // add to tail (oldest side)
            return true;
        }
        return false;
    }

    /**
     * Peek at head (oldest) without removing — useful if you only need to inspect.
     * NOTE: not atomic with takeNext; use takeNext for actual dequeue.
     */
    public Optional<String> peekHead(String queueId) {
        // oldest is at right index -1
        String head = redisTemplate.opsForList()
                .index(mainKey(queueId), -1);
        return Optional.ofNullable(head);
    }

    /**
     * optionally: method to push new item to tail (enqueue)
     */
    public void enqueueToTail(String queueId, String itemId) {
        redisTemplate.opsForList()
                .leftPush(mainKey(queueId), itemId); // push to head = newest on left
        // If you prefer pushing to tail for FIFO, use rightPush. Here design uses right
        // as oldest.
    }

    @Override
    public void deleteQueue(String queueId) {
        // delete main list, history list and init flags
        String main = mainKey(queueId);
        String history = historyKey(queueId);
        String mainFlag = main + ":init_flag";
        String historyFlag = history + ":init_flag";

        redisTemplate.delete(main);
        redisTemplate.delete(history);
        redisTemplate.delete(mainFlag);
        redisTemplate.delete(historyFlag);
        log.info("Deleted queue data for {}", queueId);
    }

}

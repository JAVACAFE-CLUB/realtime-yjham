package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.source.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 범용 보상 트랜잭션 ItemWriter
 * PENDING 상태인 데이터를 Kafka로 재발행 시도
 *
 * @param <T> Publishable을 구현한 엔티티 타입
 */
@Slf4j
public class GenericCompensatingWriter<T extends Publishable<T>> implements ItemWriter<T> {

    private static final int DEFAULT_MAX_RETRY_COUNT = 5;

    private final DataSource<T> dataSource;
    private final GenericDataPublisher publisher;
    private final CollectionMetrics metrics;
    private final int maxRetryCount;

    public GenericCompensatingWriter(DataSource<T> dataSource, GenericDataPublisher publisher, CollectionMetrics metrics) {
        this(dataSource, publisher, metrics, DEFAULT_MAX_RETRY_COUNT);
    }

    public GenericCompensatingWriter(DataSource<T> dataSource, GenericDataPublisher publisher, 
                                     CollectionMetrics metrics, int maxRetryCount) {
        this.dataSource = dataSource;
        this.publisher = publisher;
        this.metrics = metrics;
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public void write(Chunk<? extends T> chunk) {
        for (T item : chunk) {
            processItem(item);
        }
    }

    private void processItem(T item) {
        // 최대 재시도 횟수 초과 체크
        if (item.getRetryCount() >= maxRetryCount) {
            handleMaxRetryExceeded(item);
            return;
        }

        // Kafka 동기 재발행 시도
        boolean published = publisher.publishSync(
                dataSource.getTopicName(), item.getIdentifier(), item);

        if (published) {
            handlePublishSuccess(item);
        } else {
            handlePublishFailure(item);
        }
    }

    private void handleMaxRetryExceeded(T item) {
        T failedItem = item.markAsFailed();
        dataSource.save(failedItem);
        metrics.incrementCompensatingFailed(dataSource.getSourceName());
        log.error("[{}] 최대 재시도 횟수({}) 초과 - FAILED 처리: {}",
                dataSource.getSourceName(), maxRetryCount, item.getIdentifier());
    }

    private void handlePublishSuccess(T item) {
        T publishedItem = item.markAsPublished();
        dataSource.save(publishedItem);
        metrics.incrementCompensatingSuccess(dataSource.getSourceName());
        log.info("[{}] 보상 트랜잭션 성공: {}", dataSource.getSourceName(), item.getIdentifier());
    }

    private void handlePublishFailure(T item) {
        T retriedItem = item.incrementRetryCount();
        dataSource.save(retriedItem);
        log.warn("[{}] 보상 트랜잭션 실패 (재시도 {}/{}): {}",
                dataSource.getSourceName(), retriedItem.getRetryCount(), maxRetryCount, item.getIdentifier());
    }
}

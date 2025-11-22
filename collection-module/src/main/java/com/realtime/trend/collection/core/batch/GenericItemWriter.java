package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.source.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 범용 ItemWriter
 * MongoDB 저장 및 Kafka 발행 (Best Effort + Compensating Transaction 패턴)
 *
 * @param <T> Publishable을 구현한 엔티티 타입
 */
@Slf4j
public class GenericItemWriter<T extends Publishable<T>> implements ItemWriter<T> {

    private final DataSource<T> dataSource;
    private final GenericDataPublisher publisher;
    private final CollectionMetrics metrics;

    public GenericItemWriter(DataSource<T> dataSource, GenericDataPublisher publisher, CollectionMetrics metrics) {
        this.dataSource = dataSource;
        this.publisher = publisher;
        this.metrics = metrics;
    }

    @Override
    public void write(Chunk<? extends T> chunk) {
        for (T item : chunk) {
            try {
                // 1. MongoDB 저장 (PENDING 상태)
                T savedItem = dataSource.save(item);
                log.debug("[{}] 저장 완료: {}", dataSource.getSourceName(), savedItem.getIdentifier());
                metrics.incrementCollected(dataSource.getSourceName());

                // 2. Kafka 동기 발행 시도
                long startTime = System.currentTimeMillis();
                boolean published = publisher.publishSync(
                        dataSource.getTopicName(), savedItem.getIdentifier(), savedItem);
                long duration = System.currentTimeMillis() - startTime;
                metrics.recordPublishTime(dataSource.getSourceName(), duration);

                if (published) {
                    // 3. 발행 성공 시 PUBLISHED 상태로 변경
                    try {
                        T publishedItem = savedItem.markAsPublished();
                        dataSource.save(publishedItem);
                        metrics.incrementPublished(dataSource.getSourceName());
                        log.debug("[{}] 발행 완료: {}", dataSource.getSourceName(), publishedItem.getIdentifier());
                    } catch (Exception statusUpdateException) {
                        // Kafka 발행은 성공했지만 상태 변경 실패
                        // 보상 트랜잭션에서 멱등성 체크로 중복 발행 방지
                        log.error("[{}] Kafka 발행 성공했으나 상태 변경 실패 (보상 트랜잭션에서 처리): {}",
                                dataSource.getSourceName(), savedItem.getIdentifier(), statusUpdateException);
                        metrics.incrementPublished(dataSource.getSourceName());
                    }
                } else {
                    // Kafka 발행 실패 시 PENDING 상태 유지 (보상 트랜잭션에서 재시도)
                    metrics.incrementFailed(dataSource.getSourceName());
                    log.warn("[{}] Kafka 발행 실패 (PENDING 상태 유지): {}",
                            dataSource.getSourceName(), savedItem.getIdentifier());
                }

            } catch (Exception e) {
                metrics.incrementSaveFailed(dataSource.getSourceName());
                log.error("[{}] 저장 실패: {}", dataSource.getSourceName(), item.getIdentifier(), e);
                // MongoDB 저장 실패는 해당 아이템만 스킵하고 계속 진행
            }
        }
    }
}

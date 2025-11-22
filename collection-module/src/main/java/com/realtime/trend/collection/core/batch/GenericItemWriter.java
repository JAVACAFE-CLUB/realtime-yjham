package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.source.DataSource;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GenericItemWriter<T extends Publishable> implements ItemWriter<T> {

    private final DataSource<T> dataSource;
    private final GenericDataPublisher publisher;

    @Override
    public void write(Chunk<? extends T> chunk) {
        for (T item : chunk) {
            try {
                // 1. MongoDB 저장 (PENDING 상태)
                T savedItem = dataSource.save(item);
                log.debug("[{}] 저장 완료: {}", dataSource.getSourceName(), savedItem.getIdentifier());

                // 2. Kafka 발행 시도
                try {
                    publisher.publish(dataSource.getTopicName(), savedItem.getIdentifier(), savedItem);

                    // 3. 발행 성공 시 PUBLISHED 상태로 변경
                    @SuppressWarnings("unchecked")
                    T publishedItem = (T) savedItem.markAsPublished();
                    dataSource.save(publishedItem);
                    log.debug("[{}] 발행 완료: {}", dataSource.getSourceName(), publishedItem.getIdentifier());

                } catch (Exception e) {
                    // Kafka 발행 실패 시 PENDING 상태 유지 (보상 트랜잭션에서 재시도)
                    log.warn("[{}] Kafka 발행 실패 (PENDING 상태 유지): {}",
                            dataSource.getSourceName(), savedItem.getIdentifier(), e);
                }

            } catch (Exception e) {
                log.error("[{}] 저장 실패: {}", dataSource.getSourceName(), item.getIdentifier(), e);
                // MongoDB 저장 실패는 해당 아이템만 스킵하고 계속 진행
            }
        }
    }
}

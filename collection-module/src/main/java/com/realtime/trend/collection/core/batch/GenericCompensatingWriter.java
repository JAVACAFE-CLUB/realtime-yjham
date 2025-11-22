package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.source.DataSource;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GenericCompensatingWriter<T extends Publishable> implements ItemWriter<T> {

    private final DataSource<T> dataSource;
    private final GenericDataPublisher publisher;

    @Override
    public void write(Chunk<? extends T> chunk) {
        for (T item : chunk) {
            try {
                // Kafka 재발행 시도
                publisher.publish(dataSource.getTopicName(), item.getIdentifier(), item);

                // 발행 성공 시 PUBLISHED 상태로 변경
                @SuppressWarnings("unchecked")
                T publishedItem = (T) item.markAsPublished();
                dataSource.save(publishedItem);
                log.info("[{}] 보상 트랜잭션 성공: {}", dataSource.getSourceName(), item.getIdentifier());

            } catch (Exception e) {
                // 발행 실패 시 PENDING 상태 유지 (다음 실행에서 재시도)
                log.warn("[{}] 보상 트랜잭션 실패 (재시도 예정): {}",
                        dataSource.getSourceName(), item.getIdentifier(), e);
            }
        }
    }
}

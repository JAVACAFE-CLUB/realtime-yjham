package com.realtime.trend.collection.core.source;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.domain.PublishStatus;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터 소스 인터페이스
 * 새로운 데이터 소스 추가 시 이 인터페이스를 구현
 *
 * @param <T> Publishable을 구현한 엔티티 타입
 */
public interface DataSource<T extends Publishable<T>> {

    /**
     * 소스 이름 (예: "news", "youtube")
     */
    String getSourceName();

    /**
     * Kafka 토픽명
     */
    String getTopicName();

    /**
     * 수집 Job 스케줄 (cron 표현식)
     */
    String getCollectionCron();

    /**
     * 보상 트랜잭션 Job 스케줄 (cron 표현식)
     */
    String getCompensatingCron();

    /**
     * 활성화 여부
     */
    boolean isEnabled();

    /**
     * 수집용 ItemReader 생성
     */
    ItemReader<?> createCollectionReader();

    /**
     * ItemProcessor 생성 (필요 없으면 null 반환)
     */
    ItemProcessor<?, T> createProcessor();

    /**
     * 보상 트랜잭션용 Reader - PENDING 상태 데이터 조회
     */
    default ItemReader<T> createCompensatingReader() {
        return new ItemReader<>() {
            private List<T> items;
            private int index = 0;

            @Override
            public T read() {
                if (items == null) {
                    items = findPendingItems();
                }
                if (index < items.size()) {
                    return items.get(index++);
                }
                return null;
            }
        };
    }

    /**
     * PENDING 상태인 아이템 조회
     */
    List<T> findPendingItems();

    /**
     * 엔티티 저장
     */
    T save(T entity);

    /**
     * 엔티티 타입 클래스
     */
    Class<T> getEntityType();
}

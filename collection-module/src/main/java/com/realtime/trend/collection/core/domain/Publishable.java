package com.realtime.trend.collection.core.domain;

import java.time.LocalDateTime;

/**
 * 발행 가능한 엔티티 인터페이스
 * News, YouTubeVideo 등 Kafka로 발행되는 모든 엔티티가 구현
 *
 * @param <T> 구현 클래스 타입 (CRTP 패턴)
 */
public interface Publishable<T extends Publishable<T>> {

    /**
     * 고유 식별자 반환 (URL, videoId 등)
     */
    String getIdentifier();

    /**
     * 현재 발행 상태 반환
     */
    PublishStatus getPublishStatus();

    /**
     * 수집 시각 반환
     */
    LocalDateTime getCollectedAt();

    /**
     * 발행 완료 상태로 변경된 새 인스턴스 반환
     */
    T markAsPublished();

    /**
     * 재시도 횟수 반환
     */
    default int getRetryCount() {
        return 0;
    }

    /**
     * 재시도 횟수 증가된 새 인스턴스 반환
     */
    @SuppressWarnings("unchecked")
    default T incrementRetryCount() {
        return (T) this;
    }

    /**
     * 실패 상태로 변경된 새 인스턴스 반환
     */
    @SuppressWarnings("unchecked")
    default T markAsFailed() {
        return (T) this;
    }
}

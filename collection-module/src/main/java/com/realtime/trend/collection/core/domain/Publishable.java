package com.realtime.trend.collection.core.domain;

import java.time.LocalDateTime;

/**
 * 발행 가능한 엔티티 인터페이스
 * News, YouTubeVideo 등 Kafka로 발행되는 모든 엔티티가 구현
 */
public interface Publishable {

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
    Publishable markAsPublished();
}

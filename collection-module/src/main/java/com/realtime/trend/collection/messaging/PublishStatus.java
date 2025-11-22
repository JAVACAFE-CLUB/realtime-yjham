package com.realtime.trend.collection.messaging;

/**
 * Kafka 발행 상태
 */
public enum PublishStatus {
    /**
     * 발행 대기 중
     */
    PENDING,

    /**
     * 발행 완료
     */
    PUBLISHED
}

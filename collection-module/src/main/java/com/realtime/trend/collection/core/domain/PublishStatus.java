package com.realtime.trend.collection.core.domain;

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
    PUBLISHED,

    /**
     * 발행 실패 (최대 재시도 초과)
     */
    FAILED
}

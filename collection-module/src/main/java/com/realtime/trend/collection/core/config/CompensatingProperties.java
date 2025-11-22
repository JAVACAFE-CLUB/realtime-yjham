package com.realtime.trend.collection.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 보상 트랜잭션 설정 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collection.compensating")
public class CompensatingProperties {

    /**
     * 최대 재시도 횟수 (기본값: 5)
     */
    private int maxRetryCount = 5;

    /**
     * Kafka 발행 타임아웃 (초, 기본값: 10)
     */
    private long publishTimeoutSeconds = 10;
}

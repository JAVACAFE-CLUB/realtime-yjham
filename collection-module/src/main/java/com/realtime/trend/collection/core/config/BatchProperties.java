package com.realtime.trend.collection.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring Batch 설정 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collection.batch")
public class BatchProperties {

    /**
     * 청크 사이즈 (기본값: 10)
     */
    private int chunkSize = 10;

    /**
     * 스킵 제한 (기본값: 10)
     */
    private int skipLimit = 10;

    /**
     * 재시도 제한 (기본값: 3)
     */
    private int retryLimit = 3;
}

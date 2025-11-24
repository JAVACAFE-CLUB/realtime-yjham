package com.realtime.trend.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Processing 모듈 설정
 */
@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(
        int minContentLength,
        Retry retry
) {
    public record Retry(
            int maxAttempts,
            long initialInterval,
            double multiplier
    ) {}
}

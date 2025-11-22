package com.realtime.trend.serving.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-hour}")
    private int requestsPerHour;

    public Bucket resolveBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, this::createBucket);
    }

    private Bucket createBucket(String clientIp) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(requestsPerHour, Duration.ofHours(1)))
                .build();
    }

    public boolean tryConsume(String clientIp) {
        return resolveBucket(clientIp).tryConsume(1);
    }

    public long getAvailableTokens(String clientIp) {
        return resolveBucket(clientIp).getAvailableTokens();
    }
}

package com.realtime.trend.collection.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 수집 모듈 메트릭
 * Prometheus/Grafana 연동용 메트릭 제공
 */
@Slf4j
@Component
public class CollectionMetrics {

    private final MeterRegistry meterRegistry;

    // 카운터 캐시
    private final Map<String, Counter> collectedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> compensatingSuccessCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> compensatingFailedCounters = new ConcurrentHashMap<>();

    // 타이머 캐시
    private final Map<String, Timer> publishTimers = new ConcurrentHashMap<>();

    public CollectionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 수집 완료 카운터 증가
     */
    public void incrementCollected(String sourceName) {
        getCollectedCounter(sourceName).increment();
    }

    /**
     * 발행 성공 카운터 증가
     */
    public void incrementPublished(String sourceName) {
        getPublishedCounter(sourceName).increment();
    }

    /**
     * 발행 실패 카운터 증가
     */
    public void incrementFailed(String sourceName) {
        getFailedCounter(sourceName).increment();
    }

    /**
     * 보상 트랜잭션 성공 카운터 증가
     */
    public void incrementCompensatingSuccess(String sourceName) {
        getCompensatingSuccessCounter(sourceName).increment();
    }

    /**
     * 보상 트랜잭션 실패 카운터 증가
     */
    public void incrementCompensatingFailed(String sourceName) {
        getCompensatingFailedCounter(sourceName).increment();
    }

    /**
     * 발행 시간 기록
     */
    public void recordPublishTime(String sourceName, long durationMs) {
        getPublishTimer(sourceName).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Counter 헬퍼 메서드
    private Counter getCollectedCounter(String sourceName) {
        return collectedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder("collection.items.collected")
                        .description("수집된 아이템 수")
                        .tag("source", name)
                        .register(meterRegistry));
    }

    private Counter getPublishedCounter(String sourceName) {
        return publishedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder("collection.items.published")
                        .description("Kafka 발행 성공 아이템 수")
                        .tag("source", name)
                        .register(meterRegistry));
    }

    private Counter getFailedCounter(String sourceName) {
        return failedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder("collection.items.failed")
                        .description("Kafka 발행 실패 아이템 수")
                        .tag("source", name)
                        .register(meterRegistry));
    }

    private Counter getCompensatingSuccessCounter(String sourceName) {
        return compensatingSuccessCounters.computeIfAbsent(sourceName, name ->
                Counter.builder("collection.compensating.success")
                        .description("보상 트랜잭션 성공 수")
                        .tag("source", name)
                        .register(meterRegistry));
    }

    private Counter getCompensatingFailedCounter(String sourceName) {
        return compensatingFailedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder("collection.compensating.failed")
                        .description("보상 트랜잭션 실패 수")
                        .tag("source", name)
                        .register(meterRegistry));
    }

    private Timer getPublishTimer(String sourceName) {
        return publishTimers.computeIfAbsent(sourceName, name ->
                Timer.builder("collection.publish.duration")
                        .description("Kafka 발행 소요 시간")
                        .tag("source", name)
                        .register(meterRegistry));
    }
}

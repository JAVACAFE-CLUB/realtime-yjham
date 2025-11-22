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

    // 메트릭 이름 상수
    private static final String METRIC_COLLECTED = "collection.items.collected";
    private static final String METRIC_PUBLISHED = "collection.items.published";
    private static final String METRIC_FAILED = "collection.items.failed";
    private static final String METRIC_COMPENSATING_SUCCESS = "collection.compensating.success";
    private static final String METRIC_COMPENSATING_FAILED = "collection.compensating.failed";
    private static final String METRIC_PUBLISH_DURATION = "collection.publish.duration";
    private static final String METRIC_SAVE_FAILED = "collection.items.save_failed";
    private static final String METRIC_JOB_DURATION = "collection.job.duration";
    private static final String METRIC_JOB_SKIPPED = "collection.job.skipped";
    private static final String TAG_SOURCE = "source";
    private static final String TAG_JOB_TYPE = "job_type";

    private final MeterRegistry meterRegistry;

    // 카운터 캐시
    private final Map<String, Counter> collectedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> compensatingSuccessCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> compensatingFailedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> saveFailedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> jobSkippedCounters = new ConcurrentHashMap<>();

    // 타이머 캐시
    private final Map<String, Timer> publishTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> jobDurationTimers = new ConcurrentHashMap<>();

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
     * 저장 실패 카운터 증가
     */
    public void incrementSaveFailed(String sourceName) {
        getSaveFailedCounter(sourceName).increment();
    }

    /**
     * 발행 시간 기록
     */
    public void recordPublishTime(String sourceName, long durationMs) {
        getPublishTimer(sourceName).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Job 실행 시간 기록
     */
    public void recordJobDuration(String sourceName, String jobType, long durationMs) {
        getJobDurationTimer(sourceName, jobType).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 동시 실행으로 인한 Job 스킵 카운터 증가
     */
    public void incrementJobSkipped(String sourceName, String jobType) {
        getJobSkippedCounter(sourceName, jobType).increment();
    }

    // Counter 헬퍼 메서드
    private Counter getCollectedCounter(String sourceName) {
        return collectedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_COLLECTED)
                        .description("수집된 아이템 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Counter getPublishedCounter(String sourceName) {
        return publishedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_PUBLISHED)
                        .description("Kafka 발행 성공 아이템 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Counter getFailedCounter(String sourceName) {
        return failedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_FAILED)
                        .description("Kafka 발행 실패 아이템 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Counter getCompensatingSuccessCounter(String sourceName) {
        return compensatingSuccessCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_COMPENSATING_SUCCESS)
                        .description("보상 트랜잭션 성공 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Counter getCompensatingFailedCounter(String sourceName) {
        return compensatingFailedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_COMPENSATING_FAILED)
                        .description("보상 트랜잭션 실패 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Counter getSaveFailedCounter(String sourceName) {
        return saveFailedCounters.computeIfAbsent(sourceName, name ->
                Counter.builder(METRIC_SAVE_FAILED)
                        .description("MongoDB 저장 실패 수")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Timer getPublishTimer(String sourceName) {
        return publishTimers.computeIfAbsent(sourceName, name ->
                Timer.builder(METRIC_PUBLISH_DURATION)
                        .description("Kafka 발행 소요 시간")
                        .tag(TAG_SOURCE, name)
                        .register(meterRegistry));
    }

    private Timer getJobDurationTimer(String sourceName, String jobType) {
        String key = sourceName + "-" + jobType;
        return jobDurationTimers.computeIfAbsent(key, k ->
                Timer.builder(METRIC_JOB_DURATION)
                        .description("Job 실행 소요 시간")
                        .tag(TAG_SOURCE, sourceName)
                        .tag(TAG_JOB_TYPE, jobType)
                        .register(meterRegistry));
    }

    private Counter getJobSkippedCounter(String sourceName, String jobType) {
        String key = sourceName + "-" + jobType;
        return jobSkippedCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_JOB_SKIPPED)
                        .description("동시 실행으로 인해 스킵된 Job 수")
                        .tag(TAG_SOURCE, sourceName)
                        .tag(TAG_JOB_TYPE, jobType)
                        .register(meterRegistry));
    }

}

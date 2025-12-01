package com.realtime.trend.indexing.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Indexing 모듈 메트릭
 * Kafka Consumer 및 Elasticsearch 인덱싱 메트릭 제공
 */
@Slf4j
@Component
public class IndexingMetrics {

	private static final String METRIC_CONSUMED = "indexing.messages.consumed";
	private static final String METRIC_INDEXED = "indexing.messages.indexed";
	private static final String METRIC_FAILED = "indexing.messages.failed";
	private static final String METRIC_KEYWORDS_INDEXED = "indexing.keywords.indexed";
	private static final String METRIC_AGGREGATION_DURATION = "indexing.aggregation.duration";
	private static final String METRIC_INDEX_DURATION = "indexing.index.duration";
	private static final String TAG_SOURCE = "source";

	private final MeterRegistry meterRegistry;

	private final Map<String, Counter> consumedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> indexedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> keywordsIndexedCounters = new ConcurrentHashMap<>();
	private final Map<String, Timer> indexTimers = new ConcurrentHashMap<>();
	private Timer aggregationTimer;

	public IndexingMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * 메시지 소비 카운터 증가
	 */
	public void incrementConsumed(String sourceName) {
		getConsumedCounter(sourceName).increment();
	}

	/**
	 * 인덱싱 성공 카운터 증가
	 */
	public void incrementIndexed(String sourceName) {
		getIndexedCounter(sourceName).increment();
	}

	/**
	 * 인덱싱 실패 카운터 증가
	 */
	public void incrementFailed(String sourceName) {
		getFailedCounter(sourceName).increment();
	}

	/**
	 * 인덱싱된 키워드 수 증가
	 */
	public void incrementKeywordsIndexed(String sourceName, int count) {
		getKeywordsIndexedCounter(sourceName).increment(count);
	}

	/**
	 * 인덱싱 시간 기록
	 */
	public void recordIndexTime(String sourceName, long durationMs) {
		getIndexTimer(sourceName).record(durationMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * 집계 작업 시간 기록
	 */
	public void recordAggregationTime(long durationMs) {
		getAggregationTimer().record(durationMs, TimeUnit.MILLISECONDS);
	}

	private Counter getConsumedCounter(String sourceName) {
		return consumedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_CONSUMED)
				.description("소비된 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getIndexedCounter(String sourceName) {
		return indexedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_INDEXED)
				.description("인덱싱 성공 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getFailedCounter(String sourceName) {
		return failedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_FAILED)
				.description("인덱싱 실패 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getKeywordsIndexedCounter(String sourceName) {
		return keywordsIndexedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_KEYWORDS_INDEXED)
				.description("인덱싱된 키워드 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Timer getIndexTimer(String sourceName) {
		return indexTimers.computeIfAbsent(sourceName, name ->
			Timer.builder(METRIC_INDEX_DURATION)
				.description("Elasticsearch 인덱싱 소요 시간")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Timer getAggregationTimer() {
		if (aggregationTimer == null) {
			aggregationTimer = Timer.builder(METRIC_AGGREGATION_DURATION)
				.description("키워드 집계 소요 시간")
				.register(meterRegistry);
		}
		return aggregationTimer;
	}
}

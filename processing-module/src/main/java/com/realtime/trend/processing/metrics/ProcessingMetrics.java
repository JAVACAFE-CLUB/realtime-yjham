package com.realtime.trend.processing.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Processing 모듈 메트릭
 * Kafka Consumer/Producer 및 처리 파이프라인 메트릭 제공
 */
@Slf4j
@Component
public class ProcessingMetrics {

	private static final String METRIC_CONSUMED = "processing.messages.consumed";
	private static final String METRIC_PROCESSED = "processing.messages.processed";
	private static final String METRIC_FAILED = "processing.messages.failed";
	private static final String METRIC_DLQ_SENT = "processing.messages.dlq_sent";
	private static final String METRIC_PUBLISHED = "processing.messages.published";
	private static final String METRIC_PROCESS_DURATION = "processing.process.duration";
	private static final String METRIC_NER_DURATION = "processing.ner.duration";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_TOPIC = "topic";

	private final MeterRegistry meterRegistry;

	private final Map<String, Counter> consumedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> processedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> dlqSentCounters = new ConcurrentHashMap<>();
	private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();
	private final Map<String, Timer> processTimers = new ConcurrentHashMap<>();
	private final Map<String, Timer> nerTimers = new ConcurrentHashMap<>();

	public ProcessingMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * 메시지 소비 카운터 증가
	 */
	public void incrementConsumed(String sourceName) {
		getConsumedCounter(sourceName).increment();
	}

	/**
	 * 처리 성공 카운터 증가
	 */
	public void incrementProcessed(String sourceName) {
		getProcessedCounter(sourceName).increment();
	}

	/**
	 * 처리 실패 카운터 증가
	 */
	public void incrementFailed(String sourceName) {
		getFailedCounter(sourceName).increment();
	}

	/**
	 * DLQ 전송 카운터 증가
	 */
	public void incrementDlqSent(String sourceName) {
		getDlqSentCounter(sourceName).increment();
	}

	/**
	 * Kafka 발행 성공 카운터 증가
	 */
	public void incrementPublished(String sourceName, String topic) {
		getPublishedCounter(sourceName, topic).increment();
	}

	/**
	 * 처리 시간 기록
	 */
	public void recordProcessTime(String sourceName, long durationMs) {
		getProcessTimer(sourceName).record(durationMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * NER 서비스 호출 시간 기록
	 */
	public void recordNerTime(String sourceName, long durationMs) {
		getNerTimer(sourceName).record(durationMs, TimeUnit.MILLISECONDS);
	}

	private Counter getConsumedCounter(String sourceName) {
		return consumedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_CONSUMED)
				.description("소비된 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getProcessedCounter(String sourceName) {
		return processedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_PROCESSED)
				.description("처리 성공 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getFailedCounter(String sourceName) {
		return failedCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_FAILED)
				.description("처리 실패 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getDlqSentCounter(String sourceName) {
		return dlqSentCounters.computeIfAbsent(sourceName, name ->
			Counter.builder(METRIC_DLQ_SENT)
				.description("DLQ 전송 메시지 수")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Counter getPublishedCounter(String sourceName, String topic) {
		String key = sourceName + "-" + topic;
		return publishedCounters.computeIfAbsent(key, k ->
			Counter.builder(METRIC_PUBLISHED)
				.description("Kafka 발행 성공 메시지 수")
				.tag(TAG_SOURCE, sourceName)
				.tag(TAG_TOPIC, topic)
				.register(meterRegistry));
	}

	private Timer getProcessTimer(String sourceName) {
		return processTimers.computeIfAbsent(sourceName, name ->
			Timer.builder(METRIC_PROCESS_DURATION)
				.description("메시지 처리 소요 시간")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}

	private Timer getNerTimer(String sourceName) {
		return nerTimers.computeIfAbsent(sourceName, name ->
			Timer.builder(METRIC_NER_DURATION)
				.description("NER 서비스 호출 소요 시간")
				.tag(TAG_SOURCE, name)
				.register(meterRegistry));
	}
}

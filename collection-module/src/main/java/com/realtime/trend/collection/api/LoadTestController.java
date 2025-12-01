package com.realtime.trend.collection.api;

import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 부하 테스트용 API Controller
 * Kafka 파이프라인 성능 측정을 위한 더미 데이터 발행
 */
@Slf4j
@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
public class LoadTestController {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final CollectionMetrics metrics;

	@Value("${collection.news.topic:raw-news}")
	private String newsTopic;

	@Value("${collection.youtube.topic:raw-youtube}")
	private String youtubeTopic;

	/**
	 * 부하 테스트 실행
	 *
	 * @param messageCount 발행할 메시지 수
	 * @param targetTps 목표 TPS (초당 메시지 수)
	 * @param source 데이터 소스 (news, youtube, both)
	 * @return 테스트 결과
	 */
	@PostMapping("/run")
	public Map<String, Object> runLoadTest(
		@RequestParam(defaultValue = "100") int messageCount,
		@RequestParam(defaultValue = "50") int targetTps,
		@RequestParam(defaultValue = "both") String source
	) {
		log.info("부하 테스트 시작: messageCount={}, targetTps={}, source={}", messageCount, targetTps, source);

		long startTime = System.currentTimeMillis();
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		AtomicLong totalLatency = new AtomicLong(0);

		int delayMs = targetTps > 0 ? 1000 / targetTps : 0;

		boolean sendNews = "news".equals(source) || "both".equals(source);
		boolean sendYoutube = "youtube".equals(source) || "both".equals(source);

		for (int i = 0; i < messageCount; i++) {
			long messageStartTime = System.currentTimeMillis();

			try {
				if (sendNews && sendYoutube) {
					if (i % 2 == 0) {
						sendNewsMessage(i);
					} else {
						sendYoutubeMessage(i);
					}
				} else if (sendNews) {
					sendNewsMessage(i);
				} else if (sendYoutube) {
					sendYoutubeMessage(i);
				}

				successCount.incrementAndGet();
				totalLatency.addAndGet(System.currentTimeMillis() - messageStartTime);

			} catch (Exception e) {
				log.error("메시지 발행 실패 [{}]: {}", i, e.getMessage());
				failCount.incrementAndGet();
			}

			if (delayMs > 0 && i < messageCount - 1) {
				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		long endTime = System.currentTimeMillis();
		long durationMs = endTime - startTime;

		Map<String, Object> result = new HashMap<>();
		result.put("messageCount", messageCount);
		result.put("successCount", successCount.get());
		result.put("failCount", failCount.get());
		result.put("durationMs", durationMs);
		result.put("actualTps", durationMs > 0 ? (double) successCount.get() * 1000 / durationMs : 0);
		result.put("avgLatencyMs", successCount.get() > 0 ? (double) totalLatency.get() / successCount.get() : 0);
		result.put("targetTps", targetTps);
		result.put("source", source);

		log.info("부하 테스트 완료: {}", result);
		return result;
	}

	/**
	 * 비동기 부하 테스트 실행 (더 높은 TPS 달성 가능)
	 */
	@PostMapping("/run-async")
	public Map<String, Object> runAsyncLoadTest(
		@RequestParam(defaultValue = "100") int messageCount,
		@RequestParam(defaultValue = "4") int threads,
		@RequestParam(defaultValue = "both") String source
	) {
		log.info("비동기 부하 테스트 시작: messageCount={}, threads={}, source={}", messageCount, threads, source);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		long startTime = System.currentTimeMillis();
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		boolean sendNews = "news".equals(source) || "both".equals(source);
		boolean sendYoutube = "youtube".equals(source) || "both".equals(source);

		List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

		for (int i = 0; i < messageCount; i++) {
			final int index = i;
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					if (sendNews && sendYoutube) {
						if (index % 2 == 0) {
							sendNewsMessage(index);
						} else {
							sendYoutubeMessage(index);
						}
					} else if (sendNews) {
						sendNewsMessage(index);
					} else if (sendYoutube) {
						sendYoutubeMessage(index);
					}
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				}
			}, executor);
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		executor.shutdown();

		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		long endTime = System.currentTimeMillis();
		long durationMs = endTime - startTime;

		Map<String, Object> result = new HashMap<>();
		result.put("messageCount", messageCount);
		result.put("successCount", successCount.get());
		result.put("failCount", failCount.get());
		result.put("durationMs", durationMs);
		result.put("actualTps", durationMs > 0 ? (double) successCount.get() * 1000 / durationMs : 0);
		result.put("threads", threads);
		result.put("source", source);

		log.info("비동기 부하 테스트 완료: {}", result);
		return result;
	}

	private void sendNewsMessage(int index) {
		Map<String, Object> newsMessage = createNewsMessage(index);
		kafkaTemplate.send(newsTopic, "news-" + index, newsMessage);
		metrics.incrementPublished("news-loadtest");
	}

	private void sendYoutubeMessage(int index) {
		Map<String, Object> youtubeMessage = createYoutubeMessage(index);
		kafkaTemplate.send(youtubeTopic, "youtube-" + index, youtubeMessage);
		metrics.incrementPublished("youtube-loadtest");
	}

	private Map<String, Object> createNewsMessage(int index) {
		Map<String, Object> message = new HashMap<>();
		message.put("id", UUID.randomUUID().toString());
		message.put("url", "https://example.com/news/" + index);
		message.put("title", "테스트 뉴스 제목 " + index + " - 삼성전자 관련 소식");
		message.put("content", generateTestContent("news", index));
		message.put("publishedAt", LocalDateTime.now());
		message.put("publisher", "테스트 언론사");
		message.put("author", "테스트 기자");
		message.put("category", "경제");
		message.put("tags", List.of("테스트", "뉴스", "경제"));
		message.put("collectedAt", LocalDateTime.now());
		return message;
	}

	private Map<String, Object> createYoutubeMessage(int index) {
		Map<String, Object> message = new HashMap<>();
		message.put("id", UUID.randomUUID().toString());
		message.put("videoId", "video" + index + "abc");
		message.put("title", "테스트 YouTube 영상 " + index + " - BTS 신곡");
		message.put("description", generateTestContent("youtube", index));
		message.put("channelTitle", "테스트 채널");
		message.put("publishedAt", LocalDateTime.now());
		message.put("categoryId", "10");
		message.put("tags", List.of("테스트", "유튜브", "음악"));
		message.put("viewCount", (long) (Math.random() * 1000000));
		message.put("likeCount", (long) (Math.random() * 50000));
		message.put("commentCount", (long) (Math.random() * 10000));
		message.put("collectedAt", LocalDateTime.now());
		return message;
	}

	private String generateTestContent(String type, int index) {
		String[] keywords = {"삼성전자", "이재용", "반도체", "인공지능", "BTS", "아이유", "넷플릭스", "카카오", "네이버"};
		StringBuilder content = new StringBuilder();
		content.append("테스트 컨텐츠 #").append(index).append(". ");

		for (int i = 0; i < 5; i++) {
			String keyword = keywords[(index + i) % keywords.length];
			content.append(keyword).append("에 대한 내용입니다. ");
			content.append("이 문장은 ").append(type).append(" 컨텐츠의 테스트용 문장입니다. ");
		}

		return content.toString();
	}
}

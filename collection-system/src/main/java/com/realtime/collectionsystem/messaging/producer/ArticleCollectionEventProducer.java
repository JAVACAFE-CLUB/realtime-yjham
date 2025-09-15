package com.realtime.collectionsystem.messaging.producer;

import com.realtime.collectionsystem.messaging.dto.ArticleCollectionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectionEventProducer {

    private final KafkaTemplate<String, ArticleCollectionEvent> kafkaTemplate;

    private static final String TOPIC_NAME = "article-collection-events";

    public void sendArticleCollectionEvent(ArticleCollectionEvent event) {
        try {
            CompletableFuture<SendResult<String, ArticleCollectionEvent>> future =
                kafkaTemplate.send(TOPIC_NAME, event.getArticleId(), event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("기사 수집 이벤트 전송 실패: {}", event.getArticleId(), exception);
                } else {
                    log.debug("기사 수집 이벤트 전송 성공: {} to partition {}",
                            event.getArticleId(), result.getRecordMetadata().partition());
                }
            });
        } catch (Exception e) {
            log.error("기사 수집 이벤트 전송 중 예외 발생: {}", event.getArticleId(), e);
        }
    }

    public void sendArticleCollectionEvents(List<ArticleCollectionEvent> events) {
        if (events == null || events.isEmpty()) {
            log.warn("[MESSAGING] 전송할 이벤트가 없습니다");
            return;
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 소스별 통계 생성
        Map<String, Long> sourceStats = events.stream()
                .collect(Collectors.groupingBy(event -> event.getSource(), Collectors.counting()));

        String statsMessage = sourceStats.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue() + "개")
                .collect(Collectors.joining(", "));

        log.info("[MESSAGING] 기사 수집 이벤트 전송 시작 - {} (총 {}개)", statsMessage, events.size());

        List<CompletableFuture<SendResult<String, ArticleCollectionEvent>>> futures = events.stream()
                .map(event -> sendEventAsync(event, successCount, failCount))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, exception) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int success = successCount.get();
                    int fail = failCount.get();

                    if (fail == 0) {
                        log.info("[MESSAGING] 모든 이벤트 전송 완료 - {} (성공: {}개, 소요시간: {}ms)",
                                statsMessage, success, duration);
                    } else {
                        log.warn("[MESSAGING] 이벤트 전송 완료 - {} (성공: {}개, 실패: {}개, 소요시간: {}ms)",
                                statsMessage, success, fail, duration);
                    }
                });
    }

    private CompletableFuture<SendResult<String, ArticleCollectionEvent>> sendEventAsync(ArticleCollectionEvent event) {
        return kafkaTemplate.send(TOPIC_NAME, event.getArticleId(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("[MESSAGING] 이벤트 전송 실패 - ID: {}, 오류: {}", event.getArticleId(), exception.getMessage());
                    } else {
                        log.debug("[MESSAGING] 이벤트 전송 성공 - ID: {}", event.getArticleId());
                    }
                });
    }

    private CompletableFuture<SendResult<String, ArticleCollectionEvent>> sendEventAsync(
            ArticleCollectionEvent event, AtomicInteger successCount, AtomicInteger failCount) {
        return kafkaTemplate.send(TOPIC_NAME, event.getArticleId(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        failCount.incrementAndGet();
                        log.debug("[MESSAGING] 이벤트 전송 실패 - ID: {}, 오류: {}", event.getArticleId(), exception.getMessage());
                    } else {
                        successCount.incrementAndGet();
                        log.debug("[MESSAGING] 이벤트 전송 성공 - ID: {}", event.getArticleId());
                    }
                });
    }
}
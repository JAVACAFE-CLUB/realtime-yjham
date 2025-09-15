package com.realtime.collectionsystem.messaging.producer;

import com.realtime.collectionsystem.messaging.dto.ArticleCollectionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            log.warn("전송할 이벤트가 없습니다.");
            return;
        }

        log.info("기사 수집 이벤트 일괄 전송 시작: {}개", events.size());

        List<CompletableFuture<SendResult<String, ArticleCollectionEvent>>> futures = events.stream()
                .map(this::sendEventAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("일부 이벤트 전송 실패", exception);
                    } else {
                        log.info("모든 기사 수집 이벤트 전송 완료: {}개", events.size());
                    }
                });
    }

    private CompletableFuture<SendResult<String, ArticleCollectionEvent>> sendEventAsync(ArticleCollectionEvent event) {
        return kafkaTemplate.send(TOPIC_NAME, event.getArticleId(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("기사 수집 이벤트 전송 실패: {}", event.getArticleId(), exception);
                    } else {
                        log.debug("기사 수집 이벤트 전송 성공: {}", event.getArticleId());
                    }
                });
    }
}
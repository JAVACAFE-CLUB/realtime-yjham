package com.realtime.collectionsystem.messaging.producer;

import com.realtime.collectionsystem.messaging.dto.WikiPageCollectionEvent;
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
public class WikiPageCollectionEventProducer {

    private final KafkaTemplate<String, WikiPageCollectionEvent> wikiPageEventKafkaTemplate;

    private static final String TOPIC_NAME = "wikipage-collection-events";

    public void sendWikiPageCollectionEvent(WikiPageCollectionEvent event) {
        try {
            CompletableFuture<SendResult<String, WikiPageCollectionEvent>> future =
                wikiPageEventKafkaTemplate.send(TOPIC_NAME, event.getPageId(), event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("위키페이지 수집 이벤트 발송 실패 - 페이지 ID: {}, 제목: {}, 오류: {}",
                            event.getWikiPageId(), event.getTitle(), exception.getMessage());
                } else {
                    log.debug("위키페이지 수집 이벤트 발송 성공 - 페이지 ID: {}, 파티션: {}, 오프셋: {}",
                            event.getWikiPageId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("위키페이지 수집 이벤트 발송 중 예외 발생 - 페이지 ID: {}, 제목: {}",
                    event.getWikiPageId(), event.getTitle(), e);
        }
    }

    public void sendWikiPageCollectionEvents(List<WikiPageCollectionEvent> events) {
        if (events == null || events.isEmpty()) {
            log.warn("발송할 위키페이지 수집 이벤트가 없습니다.");
            return;
        }

        log.info("위키페이지 수집 이벤트 일괄 발송 시작: {}개", events.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 네임스페이스별로 그룹화해서 발송
        Map<Integer, List<WikiPageCollectionEvent>> eventsByNamespace = events.stream()
                .collect(Collectors.groupingBy(event -> event.getNamespace() != null ? event.getNamespace() : -1));

        for (Map.Entry<Integer, List<WikiPageCollectionEvent>> entry : eventsByNamespace.entrySet()) {
            Integer namespace = entry.getKey();
            List<WikiPageCollectionEvent> namespaceEvents = entry.getValue();

            log.debug("네임스페이스 {} 위키페이지 이벤트 발송: {}개", namespace, namespaceEvents.size());

            for (WikiPageCollectionEvent event : namespaceEvents) {
                try {
                    sendWikiPageCollectionEvent(event);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("위키페이지 수집 이벤트 발송 실패 - 페이지 ID: {}, 제목: {}, 오류: {}",
                            event.getWikiPageId(), event.getTitle(), e.getMessage());
                }
            }
        }

        log.info("위키페이지 수집 이벤트 일괄 발송 완료 - 성공: {}개, 실패: {}개", successCount.get(), failureCount.get());
    }
}
package com.realtime.trend.indexing.consumer;

import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProcessedContentConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProcessedContentConsumer.class);

    private final KeywordIndexingService keywordIndexingService;

    public ProcessedContentConsumer(KeywordIndexingService keywordIndexingService) {
        this.keywordIndexingService = keywordIndexingService;
    }

    @KafkaListener(topics = "${kafka.topics.processed-news}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNews(@Payload Map<String, Object> message) {
        log.debug("처리된 뉴스 수신: {}", message.get("id"));

        try {
            ProcessedMessage processedMessage = convertToProcessedMessage(message, "news");
            keywordIndexingService.indexKeywords(processedMessage);
        } catch (Exception e) {
            log.error("뉴스 색인 실패: {}", message.get("id"), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.processed-youtube}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeYoutube(@Payload Map<String, Object> message) {
        log.debug("처리된 YouTube 수신: {}", message.get("videoId"));

        try {
            ProcessedMessage processedMessage = convertToProcessedMessage(message, "youtube");
            keywordIndexingService.indexKeywords(processedMessage);
        } catch (Exception e) {
            log.error("YouTube 색인 실패: {}", message.get("videoId"), e);
        }
    }

    @SuppressWarnings("unchecked")
    private ProcessedMessage convertToProcessedMessage(Map<String, Object> message, String source) {
        String id = (String) message.get("id");
        String sourceId = source.equals("news") ? (String) message.get("url") : (String) message.get("videoId");
        String title = (String) message.get("title");
        String processedContent = source.equals("news")
                ? (String) message.get("processedContent")
                : (String) message.get("processedDescription");

        var keywordsList = (java.util.List<Map<String, String>>) message.get("keywords");
        var keywords = keywordsList != null
                ? keywordsList.stream()
                .map(k -> new ProcessedMessage.ExtractedEntity(k.get("keyword"), k.get("type")))
                .toList()
                : java.util.List.<ProcessedMessage.ExtractedEntity>of();

        return new ProcessedMessage(
                id,
                sourceId,
                title,
                processedContent,
                java.time.LocalDateTime.now(),
                keywords,
                source
        );
    }
}

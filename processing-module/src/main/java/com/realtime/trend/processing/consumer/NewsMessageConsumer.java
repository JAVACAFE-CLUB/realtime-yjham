package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.config.KafkaTopicProperties;
import com.realtime.trend.processing.dto.ProcessedNewsMessage;
import com.realtime.trend.processing.dto.RawNewsMessage;
import com.realtime.trend.processing.metrics.ProcessingMetrics;
import com.realtime.trend.processing.pipeline.ContentProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 뉴스 메시지 Consumer
 */
@Component
public class NewsMessageConsumer extends AbstractMessageConsumer<RawNewsMessage, ProcessedNewsMessage> {

    private static final String SOURCE_NAME = "news";

    private final ContentProcessingService contentProcessingService;
    private final KafkaTopicProperties topicProperties;
    private final ProcessingMetrics metrics;

    public NewsMessageConsumer(
            ContentProcessingService contentProcessingService,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaTopicProperties topicProperties,
            ProcessingMetrics metrics
    ) {
        super(kafkaTemplate);
        this.contentProcessingService = contentProcessingService;
        this.topicProperties = topicProperties;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = "${kafka.topics.raw-news}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload Map<String, Object> messageMap,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        String id = getMessageId(messageMap);
        log.debug("뉴스 메시지 수신: {}", id);
        metrics.incrementConsumed(SOURCE_NAME);
        long startTime = System.currentTimeMillis();

        try {
            RawNewsMessage message = convertToMessage(messageMap);
            Optional<ProcessedNewsMessage> processed = contentProcessingService.processNews(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(getProcessedTopic(), key, processed.get());
                metrics.incrementPublished(SOURCE_NAME, getProcessedTopic());
                log.info("뉴스 처리 완료: {} - 키워드 {}개", id, processed.get().keywords().size());
            } else {
                log.info("뉴스 필터링됨: {}", id);
            }
            metrics.incrementProcessed(SOURCE_NAME);

        } catch (Exception e) {
            log.error("뉴스 처리 실패: {} - {}", id, e.getMessage(), e);
            metrics.incrementFailed(SOURCE_NAME);
            sendToDlq(messageMap, key, e);
            metrics.incrementDlqSent(SOURCE_NAME);
            throw e;
        } finally {
            metrics.recordProcessTime(SOURCE_NAME, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    protected String getMessageId(Map<String, Object> messageMap) {
        return (String) messageMap.get("id");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RawNewsMessage convertToMessage(Map<String, Object> map) {
        return new RawNewsMessage(
                (String) map.get("id"),
                (String) map.get("url"),
                (String) map.get("title"),
                (String) map.get("content"),
                parseDateTime(map.get("publishedAt")),
                (String) map.get("publisher"),
                (String) map.get("author"),
                (String) map.get("category"),
                (List<String>) map.get("tags"),
                parseDateTime(map.get("collectedAt"))
        );
    }

    @Override
    protected String getProcessedTopic() {
        return topicProperties.processedNews();
    }

    @Override
    protected String getDlqTopic() {
        return topicProperties.dlqNews();
    }
}

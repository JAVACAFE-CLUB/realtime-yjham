package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.config.KafkaTopicProperties;
import com.realtime.trend.processing.dto.ProcessedNewsMessage;
import com.realtime.trend.processing.dto.RawNewsMessage;
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

    private final ContentProcessingService contentProcessingService;
    private final KafkaTopicProperties topicProperties;

    public NewsMessageConsumer(
            ContentProcessingService contentProcessingService,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaTopicProperties topicProperties
    ) {
        super(kafkaTemplate);
        this.contentProcessingService = contentProcessingService;
        this.topicProperties = topicProperties;
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

        try {
            RawNewsMessage message = convertToMessage(messageMap);
            Optional<ProcessedNewsMessage> processed = contentProcessingService.processNews(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(getProcessedTopic(), key, processed.get());
                log.info("뉴스 처리 완료: {} - 키워드 {}개", id, processed.get().keywords().size());
            } else {
                log.info("뉴스 필터링됨: {}", id);
            }

        } catch (Exception e) {
            log.error("뉴스 처리 실패: {} - {}", id, e.getMessage(), e);
            sendToDlq(messageMap, key, e);
            throw e;
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

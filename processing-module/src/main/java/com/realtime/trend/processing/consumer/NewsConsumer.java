package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.dto.ProcessedNewsMessage;
import com.realtime.trend.processing.dto.RawNewsMessage;
import com.realtime.trend.processing.service.ContentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class NewsConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewsConsumer.class);

    private final ContentProcessingService contentProcessingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.processed-news}")
    private String processedNewsTopic;

    @Value("${kafka.topics.dlq-news}")
    private String dlqNewsTopic;

    public NewsConsumer(
            ContentProcessingService contentProcessingService,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.contentProcessingService = contentProcessingService;
        this.kafkaTemplate = kafkaTemplate;
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
        String id = (String) messageMap.get("id");
        log.debug("뉴스 메시지 수신: {}", id);

        try {
            RawNewsMessage message = convertToRawNewsMessage(messageMap);
            Optional<ProcessedNewsMessage> processed = contentProcessingService.processNews(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(processedNewsTopic, key, processed.get());
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

    @SuppressWarnings("unchecked")
    private RawNewsMessage convertToRawNewsMessage(Map<String, Object> map) {
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

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return LocalDateTime.of(
                    ((Number) list.get(0)).intValue(),
                    ((Number) list.get(1)).intValue(),
                    ((Number) list.get(2)).intValue(),
                    ((Number) list.get(3)).intValue(),
                    ((Number) list.get(4)).intValue(),
                    list.size() > 5 ? ((Number) list.get(5)).intValue() : 0
            );
        }
        return LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private void sendToDlq(Map<String, Object> message, String key, Exception e) {
        try {
            kafkaTemplate.send(dlqNewsTopic, key, message);
            log.warn("뉴스 DLQ 전송: {}", message.get("id"));
        } catch (Exception dlqException) {
            log.error("뉴스 DLQ 전송 실패: {}", message.get("id"), dlqException);
        }
    }
}

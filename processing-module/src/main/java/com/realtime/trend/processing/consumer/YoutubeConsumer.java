package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.dto.ProcessedYoutubeMessage;
import com.realtime.trend.processing.dto.RawYoutubeMessage;
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
public class YoutubeConsumer {

    private static final Logger log = LoggerFactory.getLogger(YoutubeConsumer.class);

    private final ContentProcessingService contentProcessingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.processed-youtube}")
    private String processedYoutubeTopic;

    @Value("${kafka.topics.dlq-youtube}")
    private String dlqYoutubeTopic;

    public YoutubeConsumer(
            ContentProcessingService contentProcessingService,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.contentProcessingService = contentProcessingService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "${kafka.topics.raw-youtube}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload Map<String, Object> messageMap,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        String videoId = (String) messageMap.get("videoId");
        log.debug("YouTube 메시지 수신: {}", videoId);

        try {
            RawYoutubeMessage message = convertToRawYoutubeMessage(messageMap);
            Optional<ProcessedYoutubeMessage> processed = contentProcessingService.processYoutube(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(processedYoutubeTopic, key, processed.get());
                log.info("YouTube 처리 완료: {} - 키워드 {}개", videoId, processed.get().keywords().size());
            } else {
                log.info("YouTube 필터링됨: {}", videoId);
            }

        } catch (Exception e) {
            log.error("YouTube 처리 실패: {} - {}", videoId, e.getMessage(), e);
            sendToDlq(messageMap, key, e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private RawYoutubeMessage convertToRawYoutubeMessage(Map<String, Object> map) {
        return new RawYoutubeMessage(
                (String) map.get("id"),
                (String) map.get("videoId"),
                (String) map.get("title"),
                (String) map.get("description"),
                (String) map.get("channelTitle"),
                parseDateTime(map.get("publishedAt")),
                (String) map.get("categoryId"),
                (List<String>) map.get("tags"),
                toLong(map.get("viewCount")),
                toLong(map.get("likeCount")),
                toLong(map.get("commentCount")),
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

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private void sendToDlq(Map<String, Object> message, String key, Exception e) {
        try {
            kafkaTemplate.send(dlqYoutubeTopic, key, message);
            log.warn("YouTube DLQ 전송: {}", message.get("videoId"));
        } catch (Exception dlqException) {
            log.error("YouTube DLQ 전송 실패: {}", message.get("videoId"), dlqException);
        }
    }
}

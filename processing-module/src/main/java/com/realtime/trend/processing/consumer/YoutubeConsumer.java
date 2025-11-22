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
            @Payload RawYoutubeMessage message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.debug("YouTube 메시지 수신: {}", message.videoId());

        try {
            Optional<ProcessedYoutubeMessage> processed = contentProcessingService.processYoutube(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(processedYoutubeTopic, key, processed.get());
                log.info("YouTube 처리 완료: {} - 키워드 {}개", message.videoId(), processed.get().keywords().size());
            } else {
                log.info("YouTube 필터링됨: {}", message.videoId());
            }

        } catch (Exception e) {
            log.error("YouTube 처리 실패: {} - {}", message.videoId(), e.getMessage(), e);
            sendToDlq(message, key, e);
            throw e;
        }
    }

    private void sendToDlq(RawYoutubeMessage message, String key, Exception e) {
        try {
            kafkaTemplate.send(dlqYoutubeTopic, key, message);
            log.warn("YouTube DLQ 전송: {}", message.videoId());
        } catch (Exception dlqException) {
            log.error("YouTube DLQ 전송 실패: {}", message.videoId(), dlqException);
        }
    }
}

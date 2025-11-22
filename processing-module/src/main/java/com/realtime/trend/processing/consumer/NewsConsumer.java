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
            @Payload RawNewsMessage message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.debug("뉴스 메시지 수신: {}", message.id());

        try {
            Optional<ProcessedNewsMessage> processed = contentProcessingService.processNews(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(processedNewsTopic, key, processed.get());
                log.info("뉴스 처리 완료: {} - 키워드 {}개", message.id(), processed.get().keywords().size());
            } else {
                log.info("뉴스 필터링됨: {}", message.id());
            }

        } catch (Exception e) {
            log.error("뉴스 처리 실패: {} - {}", message.id(), e.getMessage(), e);
            sendToDlq(message, key, e);
            throw e;
        }
    }

    private void sendToDlq(RawNewsMessage message, String key, Exception e) {
        try {
            kafkaTemplate.send(dlqNewsTopic, key, message);
            log.warn("뉴스 DLQ 전송: {}", message.id());
        } catch (Exception dlqException) {
            log.error("뉴스 DLQ 전송 실패: {}", message.id(), dlqException);
        }
    }
}

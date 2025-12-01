package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.config.KafkaTopicProperties;
import com.realtime.trend.processing.dto.ProcessedYoutubeMessage;
import com.realtime.trend.processing.dto.RawYoutubeMessage;
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
 * YouTube 메시지 Consumer
 */
@Component
public class YoutubeMessageConsumer extends AbstractMessageConsumer<RawYoutubeMessage, ProcessedYoutubeMessage> {

    private static final String SOURCE_NAME = "youtube";

    private final ContentProcessingService contentProcessingService;
    private final KafkaTopicProperties topicProperties;
    private final ProcessingMetrics metrics;

    public YoutubeMessageConsumer(
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
            topics = "${kafka.topics.raw-youtube}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload Map<String, Object> messageMap,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        String videoId = getMessageId(messageMap);
        log.debug("YouTube 메시지 수신: {}", videoId);
        metrics.incrementConsumed(SOURCE_NAME);
        long startTime = System.currentTimeMillis();

        try {
            RawYoutubeMessage message = convertToMessage(messageMap);
            Optional<ProcessedYoutubeMessage> processed = contentProcessingService.processYoutube(message);

            if (processed.isPresent()) {
                kafkaTemplate.send(getProcessedTopic(), key, processed.get());
                metrics.incrementPublished(SOURCE_NAME, getProcessedTopic());
                log.info("YouTube 처리 완료: {} - 키워드 {}개", videoId, processed.get().keywords().size());
            } else {
                log.info("YouTube 필터링됨: {}", videoId);
            }
            metrics.incrementProcessed(SOURCE_NAME);

        } catch (Exception e) {
            log.error("YouTube 처리 실패: {} - {}", videoId, e.getMessage(), e);
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
        return (String) messageMap.get("videoId");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RawYoutubeMessage convertToMessage(Map<String, Object> map) {
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

    @Override
    protected String getProcessedTopic() {
        return topicProperties.processedYoutube();
    }

    @Override
    protected String getDlqTopic() {
        return topicProperties.dlqYoutube();
    }
}

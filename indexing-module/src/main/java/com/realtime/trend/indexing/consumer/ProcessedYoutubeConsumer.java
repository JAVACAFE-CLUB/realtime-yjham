package com.realtime.trend.indexing.consumer;

import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.metrics.IndexingMetrics;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 처리된 YouTube 메시지 Consumer
 */
@Component
public class ProcessedYoutubeConsumer extends AbstractIndexingConsumer {

    public ProcessedYoutubeConsumer(KeywordIndexingService keywordIndexingService, IndexingMetrics metrics) {
        super(keywordIndexingService, metrics);
    }

    @KafkaListener(
            topics = "${kafka.topics.processed-youtube}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(@Payload Map<String, Object> messageMap) {
        processAndIndex(messageMap);
    }

    @Override
    protected String getMessageId(Map<String, Object> messageMap) {
        return (String) messageMap.get("videoId");
    }

    @Override
    protected ProcessedMessage convertToProcessedMessage(Map<String, Object> messageMap) {
        return new ProcessedMessage(
                (String) messageMap.get("id"),
                (String) messageMap.get("videoId"),
                (String) messageMap.get("title"),
                (String) messageMap.get("processedDescription"),
                parseDateTime(messageMap.get("collectedAt")),
                extractKeywords(messageMap),
                getSourceType(),
                (String) messageMap.get("categoryId")
        );
    }

    @Override
    protected String getSourceType() {
        return "youtube";
    }
}

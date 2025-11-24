package com.realtime.trend.indexing.consumer;

import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 처리된 뉴스 메시지 Consumer
 */
@Component
public class ProcessedNewsConsumer extends AbstractIndexingConsumer {

    public ProcessedNewsConsumer(KeywordIndexingService keywordIndexingService) {
        super(keywordIndexingService);
    }

    @KafkaListener(
            topics = "${kafka.topics.processed-news}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(@Payload Map<String, Object> messageMap) {
        processAndIndex(messageMap);
    }

    @Override
    protected String getMessageId(Map<String, Object> messageMap) {
        return (String) messageMap.get("id");
    }

    @Override
    protected ProcessedMessage convertToProcessedMessage(Map<String, Object> messageMap) {
        return new ProcessedMessage(
                (String) messageMap.get("id"),
                (String) messageMap.get("url"),
                (String) messageMap.get("title"),
                (String) messageMap.get("processedContent"),
                parseDateTime(messageMap.get("collectedAt")),
                extractKeywords(messageMap),
                getSourceType()
        );
    }

    @Override
    protected String getSourceType() {
        return "news";
    }
}

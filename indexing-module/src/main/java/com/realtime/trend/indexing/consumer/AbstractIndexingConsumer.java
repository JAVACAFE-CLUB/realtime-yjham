package com.realtime.trend.indexing.consumer;

import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.metrics.IndexingMetrics;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 처리된 컨텐츠 Consumer 추상 클래스
 * 공통 로직(날짜 파싱, 메시지 변환 등)을 제공합니다.
 */
public abstract class AbstractIndexingConsumer {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final KeywordIndexingService keywordIndexingService;
    protected final IndexingMetrics metrics;

    protected AbstractIndexingConsumer(KeywordIndexingService keywordIndexingService, IndexingMetrics metrics) {
        this.keywordIndexingService = keywordIndexingService;
        this.metrics = metrics;
    }

    /**
     * 메시지의 고유 식별자를 반환합니다.
     */
    protected abstract String getMessageId(Map<String, Object> messageMap);

    /**
     * Map을 ProcessedMessage로 변환합니다.
     */
    protected abstract ProcessedMessage convertToProcessedMessage(Map<String, Object> messageMap);

    /**
     * 소스 타입을 반환합니다. ("news" 또는 "youtube")
     */
    protected abstract String getSourceType();

    /**
     * 메시지를 처리하고 인덱싱합니다.
     */
    protected void processAndIndex(Map<String, Object> messageMap) {
        String id = getMessageId(messageMap);
        String sourceType = getSourceType();
        log.debug("처리된 {} 수신: {}", sourceType, id);
        metrics.incrementConsumed(sourceType);
        long startTime = System.currentTimeMillis();

        try {
            ProcessedMessage processedMessage = convertToProcessedMessage(messageMap);
            keywordIndexingService.indexKeywords(processedMessage);
            metrics.incrementIndexed(sourceType);
            metrics.incrementKeywordsIndexed(sourceType, processedMessage.keywords().size());
            log.info("{} 인덱싱 완료: {}", sourceType, id);
        } catch (Exception e) {
            log.error("{} 인덱싱 실패: {} - {}", sourceType, id, e.getMessage(), e);
            metrics.incrementFailed(sourceType);
            throw e;
        } finally {
            metrics.recordIndexTime(sourceType, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 키워드 목록을 추출합니다.
     */
    @SuppressWarnings("unchecked")
    protected List<ProcessedMessage.ExtractedEntity> extractKeywords(Map<String, Object> messageMap) {
        var keywordsList = (List<Map<String, String>>) messageMap.get("keywords");
        if (keywordsList == null) {
            return List.of();
        }
        return keywordsList.stream()
                .map(k -> new ProcessedMessage.ExtractedEntity(k.get("keyword"), k.get("type")))
                .toList();
    }

    /**
     * Kafka 메시지의 날짜/시간 값을 파싱합니다.
     * JSON 역직렬화 시 LocalDateTime이 배열 형태 [year, month, day, hour, minute, second] 또는
     * ISO-8601 문자열 형태로 올 수 있습니다.
     */
    protected LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return LocalDateTime.now();
        }
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
        try {
            return LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 현재 시간 사용: {}", value);
            return LocalDateTime.now();
        }
    }
}

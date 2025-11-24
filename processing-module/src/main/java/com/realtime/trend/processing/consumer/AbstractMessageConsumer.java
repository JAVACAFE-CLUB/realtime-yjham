package com.realtime.trend.processing.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Kafka 메시지 Consumer 추상 클래스
 * 공통 로직(날짜 파싱, DLQ 전송 등)을 제공합니다.
 *
 * @param <R> Raw 메시지 타입
 * @param <P> Processed 메시지 타입
 */
public abstract class AbstractMessageConsumer<R, P> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final KafkaTemplate<String, Object> kafkaTemplate;

    protected AbstractMessageConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 메시지의 고유 식별자를 반환합니다.
     */
    protected abstract String getMessageId(Map<String, Object> messageMap);

    /**
     * Map을 Raw 메시지 객체로 변환합니다.
     */
    protected abstract R convertToMessage(Map<String, Object> messageMap);

    /**
     * 처리 완료 토픽명을 반환합니다.
     */
    protected abstract String getProcessedTopic();

    /**
     * DLQ 토픽명을 반환합니다.
     */
    protected abstract String getDlqTopic();

    /**
     * 날짜/시간 값을 파싱합니다.
     * Kafka JSON 역직렬화 시 LocalDateTime이 List 형태로 올 수 있습니다.
     */
    protected LocalDateTime parseDateTime(Object value) {
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

    /**
     * Long 값으로 변환합니다.
     */
    protected Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 실패한 메시지를 DLQ로 전송합니다.
     */
    protected void sendToDlq(Map<String, Object> message, String key, Exception e) {
        String messageId = getMessageId(message);
        try {
            kafkaTemplate.send(getDlqTopic(), key, message);
            log.warn("DLQ 전송 완료: {}", messageId);
        } catch (Exception dlqException) {
            log.error("DLQ 전송 실패: {}", messageId, dlqException);
        }
    }
}

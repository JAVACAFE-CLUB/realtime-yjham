package com.realtime.trend.collection.core.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 범용 Kafka Publisher
 * 모든 데이터 소스에서 공통으로 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericDataPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

        /**
     * 데이터를 Kafka 토픽으로 동기 발행 (결과 대기)
     *
     * @param topic   토픽명
     * @param key     메시지 키
     * @param value   메시지 값
     * @param timeout 타임아웃 (초)
     * @return 발행 성공 여부
     */
    public boolean publishSync(String topic, String key, Object value, long timeout) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, value);

            SendResult<String, Object> result = future.get(timeout, TimeUnit.SECONDS);
            log.debug("[{}] 동기 발행 성공: {} - offset: {}",
                    topic, key, result.getRecordMetadata().offset());
            return true;

        } catch (TimeoutException e) {
            log.error("[{}] 발행 타임아웃: {}", topic, key);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[{}] 발행 중단: {}", topic, key, e);
            return false;
        } catch (ExecutionException e) {
            log.error("[{}] 발행 실패: {}", topic, key, e.getCause());
            return false;
        }
    }

    /**
     * 데이터를 Kafka 토픽으로 동기 발행 (기본 타임아웃 10초)
     */
    public boolean publishSync(String topic, String key, Object value) {
        return publishSync(topic, key, value, 10);
    }
}

package com.yjham.realtime.collection.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher {


    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_NAME = "news-data";
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024;

    public void publishNewsData(NewsData newsData) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(newsData);

            if (isMessageTooLarge(jsonMessage)) {
                publishChunkedMessage(jsonMessage);
            } else {
                publishSingleMessage(jsonMessage);
            }

        } catch (JsonProcessingException e) {
            log.error("뉴스 데이터 직렬화 실패: {}", e.getMessage());
        }
    }

    private boolean isMessageTooLarge(String message) {
        return message.getBytes().length > MAX_MESSAGE_SIZE;
    }

    private void publishSingleMessage(String message) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC_NAME, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka 메시지 전송 실패", ex);
            } else {
                log.debug("Kafka 메시지 전송 성공: partition={}, offset={}",
                         result.getRecordMetadata().partition(),
                         result.getRecordMetadata().offset());
            }
        });
    }

    // 분할 전송 시 순서 보장, 전송 실패 시 후처리 등을 어떻게 해야할까
    private void publishChunkedMessage(String message) {
        log.info("메시지 크기가 커서 청크로 분할하여 전송합니다. 크기: {} bytes", message.getBytes().length);

        String[] chunks = splitMessage(message);
        for (int i = 0; i < chunks.length; i++) {
            String chunkMessage = createChunkMessage(chunks[i], i, chunks.length);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC_NAME, chunkMessage);

            final int chunkIndex = i;
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("청크 메시지 전송 실패: chunk {}/{}", chunkIndex + 1, chunks.length, ex);
                } else {
                    log.debug("청크 메시지 전송 성공: chunk {}/{}", chunkIndex + 1, chunks.length);
                }
            });
        }
    }

    private String[] splitMessage(String message) {
        int chunks = (int) Math.ceil((double) message.length() / MAX_MESSAGE_SIZE);
        String[] result = new String[chunks];

        for (int i = 0; i < chunks; i++) {
            int start = i * MAX_MESSAGE_SIZE;
            int end = Math.min(start + MAX_MESSAGE_SIZE, message.length());
            result[i] = message.substring(start, end);
        }

        return result;
    }

    private String createChunkMessage(String chunk, int chunkIndex, int totalChunks) {
        try {
            ChunkData chunkData = new ChunkData(chunkIndex, totalChunks, chunk);
            return objectMapper.writeValueAsString(chunkData);
        } catch (JsonProcessingException e) {
            log.error("청크 데이터 직렬화 실패", e);
            return String.format("{\"chunk\":%d,\"totalChunks\":%d,\"data\":\"%s\",\"error\":\"serialization_failed\"}",
                               chunkIndex, totalChunks, "");
        }
    }

    private record ChunkData(int chunk, int totalChunks, String data) {
    }
}
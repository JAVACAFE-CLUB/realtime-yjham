package com.realtime.trend.processing.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractMessageConsumer 단위 테스트")
class AbstractMessageConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TestableMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TestableMessageConsumer(kafkaTemplate);
    }

    @Nested
    @DisplayName("parseDateTime 메서드")
    class ParseDateTimeTest {

        @Test
        @DisplayName("null 입력 시 null 반환")
        void shouldReturnNullWhenInputIsNull() {
            LocalDateTime result = consumer.testParseDateTime(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("List 형태의 날짜를 LocalDateTime으로 변환 (초 포함)")
        void shouldParseListWithSeconds() {
            List<Integer> dateList = Arrays.asList(2024, 12, 25, 14, 30, 45);

            LocalDateTime result = consumer.testParseDateTime(dateList);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 25, 14, 30, 45));
        }

        @Test
        @DisplayName("List 형태의 날짜를 LocalDateTime으로 변환 (초 없음)")
        void shouldParseListWithoutSeconds() {
            List<Integer> dateList = Arrays.asList(2024, 12, 25, 14, 30);

            LocalDateTime result = consumer.testParseDateTime(dateList);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 25, 14, 30, 0));
        }

        @Test
        @DisplayName("ISO 문자열 형태의 날짜를 LocalDateTime으로 변환")
        void shouldParseIsoString() {
            String isoString = "2024-12-25T14:30:45";

            LocalDateTime result = consumer.testParseDateTime(isoString);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 25, 14, 30, 45));
        }

        @Test
        @DisplayName("ISO 문자열 (초 없음) 형태의 날짜를 LocalDateTime으로 변환")
        void shouldParseIsoStringWithoutSeconds() {
            String isoString = "2024-12-25T14:30:00";

            LocalDateTime result = consumer.testParseDateTime(isoString);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 25, 14, 30, 0));
        }

        @Test
        @DisplayName("Long 타입의 List 요소도 정상 파싱")
        void shouldParseListWithLongElements() {
            List<Number> dateList = Arrays.asList(2024L, 12L, 25L, 14L, 30L, 45L);

            LocalDateTime result = consumer.testParseDateTime(dateList);

            assertThat(result).isEqualTo(LocalDateTime.of(2024, 12, 25, 14, 30, 45));
        }

        @Test
        @DisplayName("잘못된 ISO 문자열 형식 시 예외 발생")
        void shouldThrowExceptionForInvalidIsoString() {
            String invalidIsoString = "2024/12/25 14:30:45";

            assertThatThrownBy(() -> consumer.testParseDateTime(invalidIsoString))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("toLong 메서드")
    class ToLongTest {

        @Test
        @DisplayName("null 입력 시 null 반환")
        void shouldReturnNullWhenInputIsNull() {
            Long result = consumer.testToLong(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Integer 입력을 Long으로 변환")
        void shouldConvertIntegerToLong() {
            Long result = consumer.testToLong(12345);
            assertThat(result).isEqualTo(12345L);
        }

        @Test
        @DisplayName("Long 입력 그대로 반환")
        void shouldReturnLongAsIs() {
            Long result = consumer.testToLong(9876543210L);
            assertThat(result).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("Double 입력을 Long으로 변환 (소수점 버림)")
        void shouldConvertDoubleToLong() {
            Long result = consumer.testToLong(12345.67);
            assertThat(result).isEqualTo(12345L);
        }

        @Test
        @DisplayName("숫자 문자열을 Long으로 변환")
        void shouldParseNumericString() {
            Long result = consumer.testToLong("9876543210");
            assertThat(result).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("잘못된 문자열 형식 시 예외 발생")
        void shouldThrowExceptionForInvalidString() {
            assertThatThrownBy(() -> consumer.testToLong("not-a-number"))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("sendToDlq 메서드")
    class SendToDlqTest {

        @Test
        @DisplayName("DLQ로 메시지 전송 성공")
        void shouldSendMessageToDlq() {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "test-id-123");
            message.put("content", "test content");
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            consumer.testSendToDlq(message, "test-key", new RuntimeException("Test error"));

            verify(kafkaTemplate).send("test-dlq", "test-key", message);
        }

        @Test
        @DisplayName("DLQ 전송 실패 시 에러 로깅 (예외 발생하지 않음)")
        void shouldLogErrorWhenDlqSendFails() {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "test-id-456");
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka error"));

            // 예외가 발생하지 않아야 함
            consumer.testSendToDlq(message, "test-key", new RuntimeException("Original error"));

            verify(kafkaTemplate).send("test-dlq", "test-key", message);
        }

        @Test
        @DisplayName("DLQ 전송 시 올바른 토픽으로 전송")
        void shouldSendToCorrectDlqTopic() {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "msg-789");
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            consumer.testSendToDlq(message, "key-1", new RuntimeException("Error"));

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), any());
            assertThat(topicCaptor.getValue()).isEqualTo("test-dlq");
        }
    }

    /**
     * 테스트를 위한 AbstractMessageConsumer 구현체
     */
    private static class TestableMessageConsumer extends AbstractMessageConsumer<Object, Object> {

        protected TestableMessageConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
            super(kafkaTemplate);
        }

        @Override
        protected String getMessageId(Map<String, Object> messageMap) {
            return String.valueOf(messageMap.get("id"));
        }

        @Override
        protected Object convertToMessage(Map<String, Object> messageMap) {
            return messageMap;
        }

        @Override
        protected String getProcessedTopic() {
            return "test-processed";
        }

        @Override
        protected String getDlqTopic() {
            return "test-dlq";
        }

        // protected 메서드를 테스트하기 위한 public 래퍼 메서드들
        public LocalDateTime testParseDateTime(Object value) {
            return parseDateTime(value);
        }

        public Long testToLong(Object value) {
            return toLong(value);
        }

        public void testSendToDlq(Map<String, Object> message, String key, Exception e) {
            sendToDlq(message, key, e);
        }
    }
}

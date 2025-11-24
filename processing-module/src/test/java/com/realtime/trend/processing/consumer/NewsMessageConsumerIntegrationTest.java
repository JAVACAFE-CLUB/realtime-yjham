package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.ProcessingApplication;
import com.realtime.trend.processing.config.KafkaTopicProperties;
import com.realtime.trend.processing.dto.ExtractedEntity;
import com.realtime.trend.processing.dto.RawNewsMessage;
import com.realtime.trend.processing.extraction.NerClient;
import com.realtime.trend.test.config.AbstractKafkaIntegrationTest;
import com.realtime.trend.test.kafka.KafkaTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ProcessingApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = com.realtime.trend.test.config.TestcontainersConfig.KafkaInitializer.class)
@Testcontainers
@DisplayName("NewsMessageConsumer 통합 테스트")
class NewsMessageConsumerIntegrationTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaTopicProperties topicProperties;

    @MockBean
    private NerClient nerClient;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private KafkaTestSupport kafkaTestSupport;

    @BeforeEach
    void setUp() {
        kafkaTestSupport = new KafkaTestSupport(bootstrapServers);

        // NER Client Mock 설정
        when(nerClient.analyze(anyString())).thenReturn(Arrays.asList(
                new ExtractedEntity("테스트키워드", "KEYWORD"),
                new ExtractedEntity("삼성전자", "ORG")
        ));
    }

    @Test
    @DisplayName("정상적인 뉴스 메시지 처리 - ISO 문자열 날짜 형식")
    void shouldProcessValidNewsMessageWithIsoDate() throws InterruptedException {
        // Given
        String testId = "news-iso-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawNewsMessage(
                testId,
                "테스트 뉴스 제목입니다",
                "이것은 충분히 긴 테스트 콘텐츠입니다. 뉴스 기사의 본문 내용으로 사용됩니다. 품질 검증을 통과하기 위해 충분한 길이가 필요합니다.",
                "2024-12-25T10:30:00"
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawNews(), "key-001", rawMessage);

        // Then - processed-news 토픽에서 메시지 확인
        Thread.sleep(2000); // Consumer가 메시지를 처리할 시간 확보

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedNews(),
                "test-consumer-group-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testId.equals(m.get("id")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        Map<String, Object> processed = processedMessages.get(0);
        assertThat(processed.get("id")).isEqualTo(testId);
        assertThat(processed.get("title")).isEqualTo("테스트 뉴스 제목입니다");
        assertThat((List<?>) processed.get("keywords")).isNotEmpty();
    }

    @Test
    @DisplayName("정상적인 뉴스 메시지 처리 - 배열 형식 날짜")
    void shouldProcessValidNewsMessageWithArrayDate() throws InterruptedException {
        // Given
        String testId = "news-array-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawNewsMessageWithArrayDate(
                testId,
                "배열 날짜 테스트 뉴스",
                "이것은 배열 형식 날짜를 사용하는 테스트 메시지입니다. 충분히 긴 본문 내용이 필요합니다.",
                Arrays.asList(2024, 12, 25, 14, 30, 45)
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawNews(), "key-002", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedNews(),
                "test-consumer-group-array-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testId.equals(m.get("id")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        assertThat(processedMessages.get(0).get("id")).isEqualTo(testId);
    }

    @Test
    @DisplayName("품질 검증 실패 시 메시지 필터링")
    void shouldFilterLowQualityMessage() throws InterruptedException {
        // Given - 너무 짧은 콘텐츠
        String testId = "news-short-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawNewsMessage(
                testId,
                "짧은 제목",
                "짧은 내용", // 최소 길이 미달
                "2024-12-25T10:30:00"
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawNews(), "key-short", rawMessage);

        // Then - 메시지가 필터링되어 processed 토픽에 도달하지 않음
        Thread.sleep(3000);

        // 이전 테스트들의 메시지가 있을 수 있으므로 모든 메시지를 읽고 필터링
        List<Map<String, Object>> allMessages = new java.util.ArrayList<>();
        try {
            allMessages = kafkaTestSupport.consumeMessagesAsMap(
                    topicProperties.processedNews(),
                    "test-consumer-filtered-" + System.currentTimeMillis(),
                    1,
                    Duration.ofSeconds(3)
            );
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            // 타임아웃은 메시지가 없음을 의미 - 예상된 동작
        }

        // 현재 테스트의 메시지만 필터링 - 품질 검증 실패로 없어야 함
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testId.equals(m.get("id")))
                .toList();

        assertThat(processedMessages).isEmpty();
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 DLQ로 전송")
    void shouldSendToDlqOnProcessingError() throws InterruptedException {
        // Given - NER 호출 시 예외 발생하도록 설정
        when(nerClient.analyze(anyString())).thenThrow(new RuntimeException("NER 서비스 오류"));

        String testId = "news-error-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawNewsMessage(
                testId,
                "에러 테스트 뉴스",
                "이것은 에러를 발생시키기 위한 테스트 메시지입니다. 충분히 긴 본문 내용이 필요합니다. 품질 검증을 통과해야 NER이 호출됩니다.",
                "2024-12-25T10:30:00"
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawNews(), "key-error", rawMessage);

        // Then - DLQ 토픽에서 메시지 확인
        Thread.sleep(3000);

        List<Map<String, Object>> allDlqMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.dlqNews(),
                "test-dlq-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링 (재시도로 인해 중복될 수 있으므로 존재 여부만 확인)
        List<Map<String, Object>> dlqMessages = allDlqMessages.stream()
                .filter(m -> testId.equals(m.get("id")))
                .toList();

        assertThat(dlqMessages).isNotEmpty();
        assertThat(dlqMessages.get(0).get("id")).isEqualTo(testId);
    }

    @Test
    @DisplayName("collectedAt 날짜가 처리된 메시지에 보존됨")
    void shouldPreserveCollectedAtInProcessedMessage() throws InterruptedException {
        // Given
        String testId = "news-preserve-" + System.currentTimeMillis();
        LocalDateTime collectedAt = LocalDateTime.of(2024, 12, 25, 15, 0, 0);
        Map<String, Object> rawMessage = createRawNewsMessageWithArrayDate(
                testId,
                "날짜 보존 테스트",
                "collectedAt 날짜가 올바르게 보존되는지 확인하는 테스트입니다. 충분한 길이의 본문이 필요합니다.",
                Arrays.asList(collectedAt.getYear(), collectedAt.getMonthValue(),
                        collectedAt.getDayOfMonth(), collectedAt.getHour(),
                        collectedAt.getMinute(), collectedAt.getSecond())
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawNews(), "key-preserve", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedNews(),
                "test-preserve-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testId.equals(m.get("id")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        Object collectedAtValue = processedMessages.get(0).get("collectedAt");
        assertThat(collectedAtValue).isNotNull();
    }

    private Map<String, Object> createRawNewsMessage(String id, String title, String content, String publishedAt) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", id);
        message.put("url", "https://news.example.com/" + id);
        message.put("title", title);
        message.put("content", content);
        message.put("publishedAt", publishedAt);
        message.put("publisher", "테스트뉴스");
        message.put("author", "테스트기자");
        message.put("category", "테스트");
        message.put("tags", Arrays.asList("태그1", "태그2"));
        message.put("collectedAt", publishedAt);
        return message;
    }

    private Map<String, Object> createRawNewsMessageWithArrayDate(
            String id, String title, String content, List<Integer> dateArray) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", id);
        message.put("url", "https://news.example.com/" + id);
        message.put("title", title);
        message.put("content", content);
        message.put("publishedAt", dateArray);
        message.put("publisher", "테스트뉴스");
        message.put("author", "테스트기자");
        message.put("category", "테스트");
        message.put("tags", Arrays.asList("태그1", "태그2"));
        message.put("collectedAt", dateArray);
        return message;
    }
}

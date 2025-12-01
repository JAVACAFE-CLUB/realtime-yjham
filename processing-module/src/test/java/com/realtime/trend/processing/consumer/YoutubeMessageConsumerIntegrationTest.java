package com.realtime.trend.processing.consumer;

import com.realtime.trend.processing.ProcessingApplication;
import com.realtime.trend.processing.config.KafkaTopicProperties;
import com.realtime.trend.processing.dto.ExtractedEntity;
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
@DisplayName("YoutubeMessageConsumer 통합 테스트")
class YoutubeMessageConsumerIntegrationTest extends AbstractKafkaIntegrationTest {

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
                new ExtractedEntity("유튜브채널", "ORG")
        ));
    }

    @Test
    @DisplayName("정상적인 YouTube 메시지 처리 - ISO 문자열 날짜 형식")
    void shouldProcessValidYoutubeMessageWithIsoDate() throws InterruptedException {
        // Given
        String testId = "yt-iso-" + System.currentTimeMillis();
        String testVideoId = "video-iso-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawYoutubeMessage(
                testId,
                testVideoId,
                "테스트 유튜브 제목입니다",
                "이것은 충분히 긴 테스트 설명입니다. 유튜브 영상의 설명 내용으로 사용됩니다. 품질 검증을 통과하기 위해 충분한 길이가 필요합니다.",
                "2024-12-25T10:30:00",
                100000L, 5000L, 1000L
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-001", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedYoutube(),
                "test-yt-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        Map<String, Object> processed = processedMessages.get(0);
        assertThat(processed.get("id")).isEqualTo(testId);
        assertThat(processed.get("videoId")).isEqualTo(testVideoId);
        assertThat(processed.get("title")).isEqualTo("테스트 유튜브 제목입니다");
        assertThat((List<?>) processed.get("keywords")).isNotEmpty();
    }

    @Test
    @DisplayName("정상적인 YouTube 메시지 처리 - 배열 형식 날짜")
    void shouldProcessValidYoutubeMessageWithArrayDate() throws InterruptedException {
        // Given
        String testId = "yt-array-" + System.currentTimeMillis();
        String testVideoId = "video-array-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawYoutubeMessageWithArrayDate(
                testId,
                testVideoId,
                "배열 날짜 테스트 유튜브",
                "이것은 배열 형식 날짜를 사용하는 테스트 메시지입니다. 충분히 긴 설명 내용이 필요합니다.",
                Arrays.asList(2024, 12, 25, 14, 30, 45),
                50000L, 2500L, 500L
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-002", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedYoutube(),
                "test-yt-array-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        assertThat(processedMessages.get(0).get("videoId")).isEqualTo(testVideoId);
    }

    @Test
    @DisplayName("품질 검증 실패 시 메시지 필터링")
    void shouldFilterLowQualityYoutubeMessage() throws InterruptedException {
        // Given - 너무 짧은 설명
        String testId = "yt-short-" + System.currentTimeMillis();
        String testVideoId = "video-short-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawYoutubeMessage(
                testId,
                testVideoId,
                "짧은 제목",
                "짧은 설명", // 최소 길이 미달
                "2024-12-25T10:30:00",
                1000L, 50L, 10L
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-short", rawMessage);

        // Then
        Thread.sleep(3000);

        // 이전 테스트들의 메시지가 있을 수 있으므로 모든 메시지를 읽고 필터링
        List<Map<String, Object>> allMessages = new java.util.ArrayList<>();
        try {
            allMessages = kafkaTestSupport.consumeMessagesAsMap(
                    topicProperties.processedYoutube(),
                    "test-yt-filtered-" + System.currentTimeMillis(),
                    1,
                    Duration.ofSeconds(3)
            );
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            // 타임아웃은 메시지가 없음을 의미 - 예상된 동작
        }

        // 현재 테스트의 메시지만 필터링 - 품질 검증 실패로 없어야 함
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(processedMessages).isEmpty();
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 DLQ로 전송")
    void shouldSendToDlqOnProcessingError() throws InterruptedException {
        // Given
        when(nerClient.analyze(anyString())).thenThrow(new RuntimeException("NER 서비스 오류"));

        String testId = "yt-error-" + System.currentTimeMillis();
        String testVideoId = "video-error-" + System.currentTimeMillis();
        Map<String, Object> rawMessage = createRawYoutubeMessage(
                testId,
                testVideoId,
                "에러 테스트 유튜브",
                "이것은 에러를 발생시키기 위한 테스트 메시지입니다. 충분히 긴 설명 내용이 필요합니다. 품질 검증을 통과해야 NER이 호출됩니다.",
                "2024-12-25T10:30:00",
                5000L, 100L, 50L
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-error", rawMessage);

        // Then
        Thread.sleep(3000);

        List<Map<String, Object>> allDlqMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.dlqYoutube(),
                "test-yt-dlq-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링 (재시도로 인해 중복될 수 있으므로 존재 여부만 확인)
        List<Map<String, Object>> dlqMessages = allDlqMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(dlqMessages).isNotEmpty();
        assertThat(dlqMessages.get(0).get("videoId")).isEqualTo(testVideoId);
    }

    @Test
    @DisplayName("viewCount, likeCount, commentCount가 처리된 메시지에 보존됨")
    void shouldPreserveCountersInProcessedMessage() throws InterruptedException {
        // Given
        String testId = "yt-counts-" + System.currentTimeMillis();
        String testVideoId = "video-counts-" + System.currentTimeMillis();
        Long expectedViewCount = 123456L;
        Long expectedLikeCount = 7890L;
        Long expectedCommentCount = 456L;

        Map<String, Object> rawMessage = createRawYoutubeMessage(
                testId,
                testVideoId,
                "카운터 보존 테스트",
                "viewCount, likeCount, commentCount가 올바르게 보존되는지 확인하는 테스트입니다. 충분한 길이 필요.",
                "2024-12-25T10:30:00",
                expectedViewCount, expectedLikeCount, expectedCommentCount
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-counts", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedYoutube(),
                "test-yt-counts-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        Map<String, Object> processed = processedMessages.get(0);

        // Number 타입으로 반환될 수 있으므로 longValue()로 비교
        assertThat(((Number) processed.get("viewCount")).longValue()).isEqualTo(expectedViewCount);
        assertThat(((Number) processed.get("likeCount")).longValue()).isEqualTo(expectedLikeCount);
        assertThat(((Number) processed.get("commentCount")).longValue()).isEqualTo(expectedCommentCount);
    }

    @Test
    @DisplayName("collectedAt 날짜가 처리된 메시지에 보존됨")
    void shouldPreserveCollectedAtInProcessedMessage() throws InterruptedException {
        // Given
        String testId = "yt-preserve-" + System.currentTimeMillis();
        String testVideoId = "video-preserve-" + System.currentTimeMillis();
        LocalDateTime collectedAt = LocalDateTime.of(2024, 12, 25, 15, 0, 0);
        Map<String, Object> rawMessage = createRawYoutubeMessageWithArrayDate(
                testId,
                testVideoId,
                "날짜 보존 테스트",
                "collectedAt 날짜가 올바르게 보존되는지 확인하는 테스트입니다. 충분한 길이의 설명이 필요합니다.",
                Arrays.asList(collectedAt.getYear(), collectedAt.getMonthValue(),
                        collectedAt.getDayOfMonth(), collectedAt.getHour(),
                        collectedAt.getMinute(), collectedAt.getSecond()),
                10000L, 500L, 100L
        );

        // When
        kafkaTestSupport.sendMessageAsMap(topicProperties.rawYoutube(), "key-yt-preserve", rawMessage);

        // Then
        Thread.sleep(2000);

        List<Map<String, Object>> allMessages = kafkaTestSupport.consumeMessagesAsMap(
                topicProperties.processedYoutube(),
                "test-yt-preserve-consumer-" + System.currentTimeMillis(),
                1,
                Duration.ofSeconds(10)
        );

        // 현재 테스트의 메시지만 필터링
        List<Map<String, Object>> processedMessages = allMessages.stream()
                .filter(m -> testVideoId.equals(m.get("videoId")))
                .toList();

        assertThat(processedMessages).hasSize(1);
        Object collectedAtValue = processedMessages.get(0).get("collectedAt");
        assertThat(collectedAtValue).isNotNull();
    }

    private Map<String, Object> createRawYoutubeMessage(
            String id, String videoId, String title, String description,
            String publishedAt, Long viewCount, Long likeCount, Long commentCount) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", id);
        message.put("videoId", videoId);
        message.put("title", title);
        message.put("description", description);
        message.put("channelTitle", "테스트채널");
        message.put("publishedAt", publishedAt);
        message.put("categoryId", "22");
        message.put("tags", Arrays.asList("태그1", "태그2"));
        message.put("viewCount", viewCount);
        message.put("likeCount", likeCount);
        message.put("commentCount", commentCount);
        message.put("collectedAt", publishedAt);
        return message;
    }

    private Map<String, Object> createRawYoutubeMessageWithArrayDate(
            String id, String videoId, String title, String description,
            List<Integer> dateArray, Long viewCount, Long likeCount, Long commentCount) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", id);
        message.put("videoId", videoId);
        message.put("title", title);
        message.put("description", description);
        message.put("channelTitle", "테스트채널");
        message.put("publishedAt", dateArray);
        message.put("categoryId", "22");
        message.put("tags", Arrays.asList("태그1", "태그2"));
        message.put("viewCount", viewCount);
        message.put("likeCount", likeCount);
        message.put("commentCount", commentCount);
        message.put("collectedAt", dateArray);
        return message;
    }
}

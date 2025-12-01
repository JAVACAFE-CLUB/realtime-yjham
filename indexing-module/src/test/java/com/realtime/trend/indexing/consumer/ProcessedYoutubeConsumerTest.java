package com.realtime.trend.indexing.consumer;

import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.metrics.IndexingMetrics;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedYoutubeConsumerTest {

    @Mock
    private KeywordIndexingService keywordIndexingService;

    @Mock
    private IndexingMetrics indexingMetrics;

    @Captor
    private ArgumentCaptor<ProcessedMessage> messageCaptor;

    private ProcessedYoutubeConsumer processedYoutubeConsumer;

    @BeforeEach
    void setUp() {
        processedYoutubeConsumer = new ProcessedYoutubeConsumer(keywordIndexingService, indexingMetrics);
    }

    @Test
    @DisplayName("YouTube 메시지를 수신하여 인덱싱한다")
    void consume_withValidMessage_indexesKeywords() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.id()).isEqualTo("youtube-001");
        assertThat(captured.sourceId()).isEqualTo("video123abc");
        assertThat(captured.title()).isEqualTo("BTS 신곡 발표");
        assertThat(captured.processedContent()).isEqualTo("BTS 관련 영상 설명");
        assertThat(captured.source()).isEqualTo("youtube");
        assertThat(captured.keywords()).hasSize(2);
    }

    @Test
    @DisplayName("키워드 정보를 올바르게 변환한다")
    void consume_extractsKeywordsCorrectly() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.keywords()).extracting(ProcessedMessage.ExtractedEntity::keyword)
                .containsExactly("BTS", "빌보드");
        assertThat(captured.keywords()).extracting(ProcessedMessage.ExtractedEntity::type)
                .containsExactly("ORG", "ORG");
    }

    @Test
    @DisplayName("videoId를 메시지 식별자로 사용한다")
    void consume_usesVideoIdAsIdentifier() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.sourceId()).isEqualTo("video123abc");
    }

    @Test
    @DisplayName("processedDescription을 콘텐츠로 사용한다")
    void consume_usesProcessedDescriptionAsContent() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.processedContent()).isEqualTo("BTS 관련 영상 설명");
    }

    @Test
    @DisplayName("날짜가 배열 형식이면 올바르게 파싱한다")
    void consume_withArrayDateTime_parsesCorrectly() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();
        messageMap.put("collectedAt", List.of(2024, 2, 20, 14, 0, 0));

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.collectedAt().getYear()).isEqualTo(2024);
        assertThat(captured.collectedAt().getMonthValue()).isEqualTo(2);
        assertThat(captured.collectedAt().getDayOfMonth()).isEqualTo(20);
    }

    @Test
    @DisplayName("인덱싱 실패시 예외를 전파한다")
    void consume_whenIndexingFails_throwsException() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();
        doThrow(new RuntimeException("ES 연결 실패"))
                .when(keywordIndexingService).indexKeywords(any());

        // when & then
        assertThatThrownBy(() -> processedYoutubeConsumer.consume(messageMap))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ES 연결 실패");
    }

    @Test
    @DisplayName("키워드가 없어도 정상 처리한다")
    void consume_withNoKeywords_processesSuccessfully() {
        // given
        Map<String, Object> messageMap = createYoutubeMessageMap();
        messageMap.put("keywords", null);

        // when
        processedYoutubeConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.keywords()).isEmpty();
    }

    private Map<String, Object> createYoutubeMessageMap() {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", "youtube-001");
        messageMap.put("videoId", "video123abc");
        messageMap.put("title", "BTS 신곡 발표");
        messageMap.put("processedDescription", "BTS 관련 영상 설명");
        messageMap.put("collectedAt", List.of(2024, 2, 20, 14, 0, 0));
        messageMap.put("keywords", List.of(
                Map.of("keyword", "BTS", "type", "ORG"),
                Map.of("keyword", "빌보드", "type", "ORG")
        ));
        return messageMap;
    }
}

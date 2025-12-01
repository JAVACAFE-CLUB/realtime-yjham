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
class ProcessedNewsConsumerTest {

    @Mock
    private KeywordIndexingService keywordIndexingService;

    @Mock
    private IndexingMetrics indexingMetrics;

    @Captor
    private ArgumentCaptor<ProcessedMessage> messageCaptor;

    private ProcessedNewsConsumer processedNewsConsumer;

    @BeforeEach
    void setUp() {
        processedNewsConsumer = new ProcessedNewsConsumer(keywordIndexingService, indexingMetrics);
    }

    @Test
    @DisplayName("뉴스 메시지를 수신하여 인덱싱한다")
    void consume_withValidMessage_indexesKeywords() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();

        // when
        processedNewsConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.id()).isEqualTo("news-001");
        assertThat(captured.sourceId()).isEqualTo("https://example.com/news/1");
        assertThat(captured.title()).isEqualTo("삼성전자 주가 상승");
        assertThat(captured.processedContent()).isEqualTo("삼성전자 관련 뉴스 내용");
        assertThat(captured.source()).isEqualTo("news");
        assertThat(captured.keywords()).hasSize(2);
    }

    @Test
    @DisplayName("키워드 정보를 올바르게 변환한다")
    void consume_extractsKeywordsCorrectly() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();

        // when
        processedNewsConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.keywords()).extracting(ProcessedMessage.ExtractedEntity::keyword)
                .containsExactly("삼성전자", "이재용");
        assertThat(captured.keywords()).extracting(ProcessedMessage.ExtractedEntity::type)
                .containsExactly("ORG", "PER");
    }

    @Test
    @DisplayName("날짜가 배열 형식이면 올바르게 파싱한다")
    void consume_withArrayDateTime_parsesCorrectly() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();
        messageMap.put("collectedAt", List.of(2024, 1, 15, 10, 30, 0));

        // when
        processedNewsConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.collectedAt().getYear()).isEqualTo(2024);
        assertThat(captured.collectedAt().getMonthValue()).isEqualTo(1);
        assertThat(captured.collectedAt().getDayOfMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("날짜가 문자열 형식이면 올바르게 파싱한다")
    void consume_withStringDateTime_parsesCorrectly() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();
        messageMap.put("collectedAt", "2024-01-15T10:30:00");

        // when
        processedNewsConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.collectedAt().getYear()).isEqualTo(2024);
        assertThat(captured.collectedAt().getMonthValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("인덱싱 실패시 예외를 전파한다")
    void consume_whenIndexingFails_throwsException() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();
        doThrow(new RuntimeException("ES 연결 실패"))
                .when(keywordIndexingService).indexKeywords(any());

        // when & then
        assertThatThrownBy(() -> processedNewsConsumer.consume(messageMap))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ES 연결 실패");
    }

    @Test
    @DisplayName("키워드가 없어도 정상 처리한다")
    void consume_withNoKeywords_processesSuccessfully() {
        // given
        Map<String, Object> messageMap = createNewsMessageMap();
        messageMap.put("keywords", null);

        // when
        processedNewsConsumer.consume(messageMap);

        // then
        verify(keywordIndexingService).indexKeywords(messageCaptor.capture());
        ProcessedMessage captured = messageCaptor.getValue();

        assertThat(captured.keywords()).isEmpty();
    }

    private Map<String, Object> createNewsMessageMap() {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", "news-001");
        messageMap.put("url", "https://example.com/news/1");
        messageMap.put("title", "삼성전자 주가 상승");
        messageMap.put("processedContent", "삼성전자 관련 뉴스 내용");
        messageMap.put("collectedAt", List.of(2024, 1, 15, 10, 30, 0));
        messageMap.put("keywords", List.of(
                Map.of("keyword", "삼성전자", "type", "ORG"),
                Map.of("keyword", "이재용", "type", "PER")
        ));
        return messageMap;
    }
}

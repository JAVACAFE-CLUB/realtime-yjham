package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.document.KeywordDocument;
import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordIndexingServiceTest {

    @Mock
    private KeywordRepository keywordRepository;

    @Captor
    private ArgumentCaptor<List<KeywordDocument>> documentsCaptor;

    private KeywordIndexingService keywordIndexingService;

    @BeforeEach
    void setUp() {
        keywordIndexingService = new KeywordIndexingService(keywordRepository);
        ReflectionTestUtils.setField(keywordIndexingService, "retentionDays", 7);
    }

    @Test
    @DisplayName("키워드가 있는 메시지를 인덱싱한다")
    void indexKeywords_withKeywords_savesDocuments() {
        // given
        LocalDateTime collectedAt = LocalDateTime.now();
        List<ProcessedMessage.ExtractedEntity> keywords = List.of(
                new ProcessedMessage.ExtractedEntity("삼성전자", "ORG"),
                new ProcessedMessage.ExtractedEntity("이재용", "PER")
        );
        ProcessedMessage message = new ProcessedMessage(
                "news-001",
                "https://example.com/news/1",
                "삼성전자 뉴스",
                "삼성전자 관련 뉴스 내용",
                collectedAt,
                keywords,
                "news"
        );

        // when
        keywordIndexingService.indexKeywords(message);

        // then
        verify(keywordRepository).saveAll(documentsCaptor.capture());
        List<KeywordDocument> savedDocuments = documentsCaptor.getValue();

        assertThat(savedDocuments).hasSize(2);
        assertThat(savedDocuments.get(0).getKeyword()).isEqualTo("삼성전자");
        assertThat(savedDocuments.get(0).getType()).isEqualTo("ORG");
        assertThat(savedDocuments.get(0).getSource()).isEqualTo("news");
        assertThat(savedDocuments.get(0).getSourceId()).isEqualTo("https://example.com/news/1");
        assertThat(savedDocuments.get(1).getKeyword()).isEqualTo("이재용");
        assertThat(savedDocuments.get(1).getType()).isEqualTo("PER");
    }

    @Test
    @DisplayName("키워드가 없는 메시지는 인덱싱하지 않는다")
    void indexKeywords_withEmptyKeywords_doesNotSave() {
        // given
        ProcessedMessage message = new ProcessedMessage(
                "news-002",
                "https://example.com/news/2",
                "뉴스 제목",
                "뉴스 내용",
                LocalDateTime.now(),
                List.of(),
                "news"
        );

        // when
        keywordIndexingService.indexKeywords(message);

        // then
        verify(keywordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("키워드가 null인 메시지는 인덱싱하지 않는다")
    void indexKeywords_withNullKeywords_doesNotSave() {
        // given
        ProcessedMessage message = new ProcessedMessage(
                "news-003",
                "https://example.com/news/3",
                "뉴스 제목",
                "뉴스 내용",
                LocalDateTime.now(),
                null,
                "news"
        );

        // when
        keywordIndexingService.indexKeywords(message);

        // then
        verify(keywordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("오래된 키워드를 삭제한다")
    void deleteOldKeywords_deletesOldDocuments() {
        // given
        when(keywordRepository.deleteByCollectedAtBefore(any(LocalDateTime.class))).thenReturn(10L);

        // when
        keywordIndexingService.deleteOldKeywords();

        // then
        verify(keywordRepository).deleteByCollectedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("YouTube 메시지를 인덱싱한다")
    void indexKeywords_withYoutubeMessage_savesDocuments() {
        // given
        LocalDateTime collectedAt = LocalDateTime.now();
        List<ProcessedMessage.ExtractedEntity> keywords = List.of(
                new ProcessedMessage.ExtractedEntity("BTS", "ORG")
        );
        ProcessedMessage message = new ProcessedMessage(
                "youtube-001",
                "video123",
                "BTS 관련 영상",
                "BTS 영상 설명",
                collectedAt,
                keywords,
                "youtube"
        );

        // when
        keywordIndexingService.indexKeywords(message);

        // then
        verify(keywordRepository).saveAll(documentsCaptor.capture());
        List<KeywordDocument> savedDocuments = documentsCaptor.getValue();

        assertThat(savedDocuments).hasSize(1);
        assertThat(savedDocuments.get(0).getSource()).isEqualTo("youtube");
        assertThat(savedDocuments.get(0).getSourceId()).isEqualTo("video123");
    }
}

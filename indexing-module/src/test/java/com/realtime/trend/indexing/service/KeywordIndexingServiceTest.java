package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.document.KeywordDocument;
import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("KeywordIndexingService 테스트")
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
        ReflectionTestUtils.setField(keywordIndexingService, "retentionDays", 30);
    }

    @Nested
    @DisplayName("indexKeywords 메서드")
    class IndexKeywordsMethod {

        @Test
        @DisplayName("유효한 메시지의 키워드 색인")
        void shouldIndexKeywordsFromValidMessage() {
            // given
            ProcessedMessage message = createProcessedMessage(List.of(
                    new ProcessedMessage.ExtractedEntity("삼성전자", "ORGANIZATION"),
                    new ProcessedMessage.ExtractedEntity("서울", "LOCATION")
            ));

            // when
            keywordIndexingService.indexKeywords(message);

            // then
            verify(keywordRepository).saveAll(documentsCaptor.capture());
            List<KeywordDocument> savedDocuments = documentsCaptor.getValue();

            assertThat(savedDocuments).hasSize(2);
            assertThat(savedDocuments).extracting(KeywordDocument::keyword)
                    .containsExactly("삼성전자", "서울");
            assertThat(savedDocuments).extracting(KeywordDocument::type)
                    .containsExactly("ORGANIZATION", "LOCATION");
            assertThat(savedDocuments).extracting(KeywordDocument::source)
                    .containsOnly("news");
        }

        @Test
        @DisplayName("키워드가 없는 메시지는 색인하지 않음")
        void shouldNotIndexWhenNoKeywords() {
            // given
            ProcessedMessage message = createProcessedMessage(List.of());

            // when
            keywordIndexingService.indexKeywords(message);

            // then
            verify(keywordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("키워드가 null인 메시지는 색인하지 않음")
        void shouldNotIndexWhenKeywordsNull() {
            // given
            ProcessedMessage message = createProcessedMessage(null);

            // when
            keywordIndexingService.indexKeywords(message);

            // then
            verify(keywordRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("deleteOldKeywords 메서드")
    class DeleteOldKeywordsMethod {

        @Test
        @DisplayName("보존 기간 이전 키워드 삭제")
        void shouldDeleteOldKeywords() {
            // given
            when(keywordRepository.deleteByCollectedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(100L);

            // when
            keywordIndexingService.deleteOldKeywords();

            // then
            verify(keywordRepository).deleteByCollectedAtBefore(any(LocalDateTime.class));
        }
    }

    private ProcessedMessage createProcessedMessage(List<ProcessedMessage.ExtractedEntity> keywords) {
        return new ProcessedMessage(
                "msg-001",
                "https://example.com/news/1",
                "테스트 뉴스 제목",
                "테스트 뉴스 본문입니다.",
                LocalDateTime.now(),
                keywords,
                "news"
        );
    }
}

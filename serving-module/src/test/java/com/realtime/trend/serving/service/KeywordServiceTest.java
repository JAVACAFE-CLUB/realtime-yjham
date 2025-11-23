package com.realtime.trend.serving.service;

import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("KeywordService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private ElasticsearchKeywordService elasticsearchService;

    private KeywordService keywordService;

    @BeforeEach
    void setUp() {
        keywordService = new KeywordService(elasticsearchService);
    }

    @Nested
    @DisplayName("getKeywords 메서드")
    class GetKeywordsTest {

        @Test
        @DisplayName("Elasticsearch에서 키워드를 조회하여 반환해야 한다")
        void shouldReturnKeywordsFromElasticsearch() {
            // given
            String source = "news";
            String type = "PERSON";
            int limit = 10;

            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("김연아", "PERSON", 100),
                    new KeywordItem("손흥민", "PERSON", 80)
            );

            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            KeywordResponse response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(2);
            assertThat(response.keywords().get(0).keyword()).isEqualTo("김연아");
            assertThat(response.keywords().get(1).keyword()).isEqualTo("손흥민");

            verify(elasticsearchService).getKeywords(source, type, limit);
        }

        @Test
        @DisplayName("메타데이터가 올바르게 생성되어야 한다")
        void shouldCreateCorrectMetadata() {
            // given
            String source = "youtube";
            String type = "ORGANIZATION";
            int limit = 5;

            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("삼성전자", "ORGANIZATION", 150)
            );

            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            KeywordResponse response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.metadata().totalCount()).isEqualTo(1);
            assertThat(response.metadata().limit()).isEqualTo(limit);
            assertThat(response.metadata().source()).isEqualTo(source);
            assertThat(response.metadata().type()).isEqualTo(type);
            assertThat(response.metadata().lastUpdated()).isNotNull();
        }

        @Test
        @DisplayName("키워드가 없을 때 빈 목록을 반환해야 한다")
        void shouldReturnEmptyListWhenNoKeywords() {
            // given
            String source = "all";
            String type = "all";
            int limit = 10;

            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(Collections.emptyList());

            // when
            KeywordResponse response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).isEmpty();
            assertThat(response.metadata().totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("다양한 source와 type 조합으로 조회할 수 있어야 한다")
        void shouldHandleVariousSourceAndTypeCombinations() {
            // given
            String source = "all";
            String type = "LOCATION";
            int limit = 3;

            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("서울", "LOCATION", 200),
                    new KeywordItem("부산", "LOCATION", 150),
                    new KeywordItem("대구", "LOCATION", 100)
            );

            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            KeywordResponse response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(3);
            assertThat(response.metadata().source()).isEqualTo("all");
            assertThat(response.metadata().type()).isEqualTo("LOCATION");
        }
    }
}

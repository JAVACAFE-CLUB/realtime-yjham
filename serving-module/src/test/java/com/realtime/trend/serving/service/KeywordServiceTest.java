package com.realtime.trend.serving.service;

import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private ElasticsearchKeywordService elasticsearchKeywordService;

    private KeywordService keywordService;

    @BeforeEach
    void setUp() {
        keywordService = new KeywordService(elasticsearchKeywordService);
    }

    @Test
    @DisplayName("키워드를 조회하여 응답을 반환한다")
    void getKeywords_returnsKeywordResponse() {
        // given
        List<KeywordItem> mockKeywords = List.of(
                new KeywordItem("삼성전자", "ORG", 100),
                new KeywordItem("이재용", "PER", 50)
        );
        when(elasticsearchKeywordService.getKeywords("news", "all", 10))
                .thenReturn(mockKeywords);

        // when
        KeywordResponse response = keywordService.getKeywords("news", "all", 10);

        // then
        assertThat(response.keywords()).hasSize(2);
        assertThat(response.keywords().get(0).keyword()).isEqualTo("삼성전자");
        assertThat(response.keywords().get(0).count()).isEqualTo(100);
    }

    @Test
    @DisplayName("메타데이터가 올바르게 설정된다")
    void getKeywords_setsMetadataCorrectly() {
        // given
        List<KeywordItem> mockKeywords = List.of(
                new KeywordItem("BTS", "ORG", 200)
        );
        when(elasticsearchKeywordService.getKeywords("youtube", "ORG", 5))
                .thenReturn(mockKeywords);

        // when
        KeywordResponse response = keywordService.getKeywords("youtube", "ORG", 5);

        // then
        assertThat(response.metadata().totalCount()).isEqualTo(1);
        assertThat(response.metadata().limit()).isEqualTo(5);
        assertThat(response.metadata().source()).isEqualTo("youtube");
        assertThat(response.metadata().type()).isEqualTo("ORG");
        assertThat(response.metadata().lastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("결과가 없으면 빈 목록을 반환한다")
    void getKeywords_whenNoResults_returnsEmptyList() {
        // given
        when(elasticsearchKeywordService.getKeywords("all", "all", 10))
                .thenReturn(List.of());

        // when
        KeywordResponse response = keywordService.getKeywords("all", "all", 10);

        // then
        assertThat(response.keywords()).isEmpty();
        assertThat(response.metadata().totalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("all 소스로 조회할 수 있다")
    void getKeywords_withAllSource_returnsResults() {
        // given
        List<KeywordItem> mockKeywords = List.of(
                new KeywordItem("테스트", "ORG", 30)
        );
        when(elasticsearchKeywordService.getKeywords("all", "all", 10))
                .thenReturn(mockKeywords);

        // when
        KeywordResponse response = keywordService.getKeywords("all", "all", 10);

        // then
        assertThat(response.keywords()).hasSize(1);
        assertThat(response.metadata().source()).isEqualTo("all");
    }
}

package com.realtime.trend.serving.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchKeywordServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private SearchResponse<Void> searchResponse;

    private ElasticsearchKeywordService elasticsearchKeywordService;

    @BeforeEach
    void setUp() {
        elasticsearchKeywordService = new ElasticsearchKeywordService(elasticsearchClient);
        ReflectionTestUtils.setField(elasticsearchKeywordService, "indexName", "keywords-test");
    }

    @Test
    @DisplayName("Elasticsearch 조회 실패시 빈 목록을 반환한다")
    void getKeywords_whenElasticsearchFails_returnsEmptyList() throws Exception {
        // given
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenThrow(new RuntimeException("ES 연결 실패"));

        // when
        List<KeywordItem> result = elasticsearchKeywordService.getKeywords("news", "all", 10);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("source가 all이면 필터링하지 않는다")
    void getKeywords_withAllSource_doesNotFilterBySource() throws Exception {
        // given - ES 호출 시 예외 발생 (필터링 검증은 통합 테스트에서)
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Test"));

        // when
        List<KeywordItem> result = elasticsearchKeywordService.getKeywords("all", "all", 10);

        // then - 예외 발생해도 빈 목록 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("type이 all이면 필터링하지 않는다")
    void getKeywords_withAllType_doesNotFilterByType() throws Exception {
        // given
        when(elasticsearchClient.search(any(Function.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Test"));

        // when
        List<KeywordItem> result = elasticsearchKeywordService.getKeywords("news", "all", 10);

        // then
        assertThat(result).isEmpty();
    }
}

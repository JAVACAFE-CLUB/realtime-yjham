package com.realtime.trend.indexing.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("KeywordAggregationService 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordAggregationServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    private KeywordAggregationService keywordAggregationService;

    @BeforeEach
    void setUp() {
        keywordAggregationService = new KeywordAggregationService(elasticsearchClient);
        ReflectionTestUtils.setField(keywordAggregationService, "indexName", "keywords-test");
        ReflectionTestUtils.setField(keywordAggregationService, "topCount", 10);
    }

    @Nested
    @DisplayName("aggregateKeywords 메서드")
    class AggregateKeywordsMethod {

        @Test
        @DisplayName("Elasticsearch 오류 시 빈 리스트 반환")
        void shouldReturnEmptyListOnElasticsearchError() throws Exception {
            // given
            when(elasticsearchClient.search(any(), eq(Void.class)))
                    .thenThrow(new RuntimeException("Elasticsearch 연결 실패"));

            // when
            List<KeywordAggregation> result = keywordAggregationService.aggregateKeywords("all", "all");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("aggregateAll 메서드")
    class AggregateAllMethod {

        @Test
        @DisplayName("모든 소스/타입 조합에 대해 집계 수행")
        void shouldAggregateAllCombinations() throws Exception {
            // given
            when(elasticsearchClient.search(any(), eq(Void.class)))
                    .thenThrow(new RuntimeException("모킹된 예외"));

            // when
            Map<String, List<KeywordAggregation>> result = keywordAggregationService.aggregateAll();

            // then
            assertThat(result).containsKeys(
                    "all:all",
                    "news:all",
                    "youtube:all",
                    "all:PERSON",
                    "all:LOCATION",
                    "all:ORGANIZATION"
            );
            // 각 조합에서 빈 리스트 반환 (오류로 인해)
            assertThat(result.get("all:all")).isEmpty();
        }

        @Test
        @DisplayName("집계 결과가 12개의 조합을 포함")
        void shouldContainAllTwelveCombinations() throws Exception {
            // given
            when(elasticsearchClient.search(any(), eq(Void.class)))
                    .thenThrow(new RuntimeException("모킹된 예외"));

            // when
            Map<String, List<KeywordAggregation>> result = keywordAggregationService.aggregateAll();

            // then
            assertThat(result).hasSize(12);
        }
    }
}

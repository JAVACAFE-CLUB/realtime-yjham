package com.realtime.trend.serving.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("ElasticsearchKeywordService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ElasticsearchKeywordServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    private ElasticsearchKeywordService service;

    @BeforeEach
    void setUp() {
        service = new ElasticsearchKeywordService(elasticsearchClient);
        ReflectionTestUtils.setField(service, "indexName", "keywords");
    }

    @Nested
    @DisplayName("getKeywords 메서드")
    class GetKeywordsTest {

        @Test
        @DisplayName("키워드 집계 결과를 올바르게 반환해야 한다")
        @SuppressWarnings("unchecked")
        void shouldReturnAggregatedKeywords() throws IOException {
            // given
            String source = "news";
            String type = "PERSON";
            int limit = 10;

            SearchResponse<Void> mockResponse = createMockSearchResponse(
                    List.of(
                            createMockBucket("김연아", 100, "PERSON"),
                            createMockBucket("손흥민", 80, "PERSON")
                    )
            );

            given(elasticsearchClient.search(any(java.util.function.Function.class), eq(Void.class)))
                    .willReturn(mockResponse);

            // when
            var result = service.getKeywords(source, type, limit);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).keyword()).isEqualTo("김연아");
            assertThat(result.get(0).count()).isEqualTo(100);
            assertThat(result.get(0).type()).isEqualTo("PERSON");
            assertThat(result.get(1).keyword()).isEqualTo("손흥민");
            assertThat(result.get(1).count()).isEqualTo(80);
        }

        @Test
        @DisplayName("source가 'all'이면 필터 없이 조회해야 한다")
        @SuppressWarnings("unchecked")
        void whenSourceIsAll_shouldQueryWithoutSourceFilter() throws IOException {
            // given
            SearchResponse<Void> mockResponse = createMockSearchResponse(
                    List.of(createMockBucket("트렌드", 50, "ORGANIZATION"))
            );

            given(elasticsearchClient.search(any(java.util.function.Function.class), eq(Void.class)))
                    .willReturn(mockResponse);

            // when
            var result = service.getKeywords("all", "all", 10);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Elasticsearch 오류 발생 시 빈 리스트를 반환해야 한다")
        @SuppressWarnings("unchecked")
        void whenElasticsearchError_shouldReturnEmptyList() throws IOException {
            // given
            given(elasticsearchClient.search(any(java.util.function.Function.class), eq(Void.class)))
                    .willThrow(new IOException("Connection failed"));

            // when
            var result = service.getKeywords("news", "PERSON", 10);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("type_info 집계가 비어있으면 UNKNOWN 타입을 반환해야 한다")
        @SuppressWarnings("unchecked")
        void whenTypeInfoEmpty_shouldReturnUnknownType() throws IOException {
            // given
            SearchResponse<Void> mockResponse = createMockSearchResponse(
                    List.of(createMockBucketWithoutType("테스트키워드", 30))
            );

            given(elasticsearchClient.search(any(java.util.function.Function.class), eq(Void.class)))
                    .willReturn(mockResponse);

            // when
            var result = service.getKeywords("news", "all", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환해야 한다")
        @SuppressWarnings("unchecked")
        void whenNoResults_shouldReturnEmptyList() throws IOException {
            // given
            SearchResponse<Void> mockResponse = createMockSearchResponse(List.of());

            given(elasticsearchClient.search(any(java.util.function.Function.class), eq(Void.class)))
                    .willReturn(mockResponse);

            // when
            var result = service.getKeywords("news", "PERSON", 10);

            // then
            assertThat(result).isEmpty();
        }
    }

    @SuppressWarnings("unchecked")
    private SearchResponse<Void> createMockSearchResponse(List<StringTermsBucket> buckets) {
        SearchResponse<Void> response = mock(SearchResponse.class);

        StringTermsAggregate termsAggregate = mock(StringTermsAggregate.class);
        var bucketsWrapper = mock(co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
        given(bucketsWrapper.array()).willReturn(buckets);
        given(termsAggregate.buckets()).willReturn(bucketsWrapper);

        Aggregate aggregate = mock(Aggregate.class);
        given(aggregate.sterms()).willReturn(termsAggregate);

        Map<String, Aggregate> aggregations = Map.of("top_keywords", aggregate);
        given(response.aggregations()).willReturn(aggregations);

        return response;
    }

    @SuppressWarnings("unchecked")
    private StringTermsBucket createMockBucket(String keyword, long count, String type) {
        StringTermsBucket bucket = mock(StringTermsBucket.class);

        var keyValue = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
        given(keyValue.stringValue()).willReturn(keyword);
        given(bucket.key()).willReturn(keyValue);
        given(bucket.docCount()).willReturn(count);

        // type_info aggregation
        StringTermsAggregate typeAggregate = mock(StringTermsAggregate.class);
        StringTermsBucket typeBucket = mock(StringTermsBucket.class);
        var typeKeyValue = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
        given(typeKeyValue.stringValue()).willReturn(type);
        given(typeBucket.key()).willReturn(typeKeyValue);

        var typeBucketsWrapper = mock(co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
        given(typeBucketsWrapper.array()).willReturn(List.of(typeBucket));
        given(typeAggregate.buckets()).willReturn(typeBucketsWrapper);

        Aggregate typeAgg = mock(Aggregate.class);
        given(typeAgg.sterms()).willReturn(typeAggregate);

        Map<String, Aggregate> subAggregations = Map.of("type_info", typeAgg);
        given(bucket.aggregations()).willReturn(subAggregations);

        return bucket;
    }

    @SuppressWarnings("unchecked")
    private StringTermsBucket createMockBucketWithoutType(String keyword, long count) {
        StringTermsBucket bucket = mock(StringTermsBucket.class);

        var keyValue = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
        given(keyValue.stringValue()).willReturn(keyword);
        given(bucket.key()).willReturn(keyValue);
        given(bucket.docCount()).willReturn(count);

        // Empty type_info aggregation
        StringTermsAggregate typeAggregate = mock(StringTermsAggregate.class);
        var typeBucketsWrapper = mock(co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
        given(typeBucketsWrapper.array()).willReturn(List.of());
        given(typeAggregate.buckets()).willReturn(typeBucketsWrapper);

        Aggregate typeAgg = mock(Aggregate.class);
        given(typeAgg.sterms()).willReturn(typeAggregate);

        Map<String, Aggregate> subAggregations = Map.of("type_info", typeAgg);
        given(bucket.aggregations()).willReturn(subAggregations);

        return bucket;
    }
}

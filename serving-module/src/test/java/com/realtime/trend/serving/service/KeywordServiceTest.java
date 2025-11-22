package com.realtime.trend.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("KeywordService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ElasticsearchKeywordService elasticsearchService;

    private ObjectMapper objectMapper;
    private KeywordService keywordService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        keywordService = new KeywordService(redisTemplate, elasticsearchService, objectMapper);
        ReflectionTestUtils.setField(keywordService, "keyPrefix", "keywords:today");
        ReflectionTestUtils.setField(keywordService, "ttlMinutes", 10);
    }

    @Nested
    @DisplayName("getKeywords 메서드")
    class GetKeywordsTest {

        @Test
        @DisplayName("Redis 캐시 히트 시 캐시된 데이터를 반환해야 한다")
        void whenCacheHit_shouldReturnCachedData() throws JsonProcessingException {
            // given
            String source = "news";
            String type = "PERSON";
            int limit = 10;

            List<KeywordItem> cachedKeywords = List.of(
                    new KeywordItem("김연아", "PERSON", 100),
                    new KeywordItem("손흥민", "PERSON", 80)
            );
            String cachedJson = objectMapper.writeValueAsString(cachedKeywords);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("keywords:today:source:news:type:PERSON:limit:10")).willReturn(cachedJson);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(2);
            assertThat(response.keywords().get(0).keyword()).isEqualTo("김연아");
            assertThat(response.metadata().source()).isEqualTo(source);
            assertThat(response.metadata().type()).isEqualTo(type);

            verify(elasticsearchService, never()).getKeywords(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("Redis 캐시 미스 시 Elasticsearch에서 조회하고 캐싱해야 한다")
        void whenCacheMiss_shouldQueryElasticsearchAndCache() throws JsonProcessingException {
            // given
            String source = "youtube";
            String type = "all";
            int limit = 5;

            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("트렌드", "ORGANIZATION", 50),
                    new KeywordItem("뉴스", "ORGANIZATION", 30)
            );

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn(null);
            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(2);
            verify(elasticsearchService).getKeywords(source, type, limit);
            verify(valueOperations).set(
                    eq("keywords:today:source:youtube:type:all:limit:5"),
                    anyString(),
                    eq(Duration.ofMinutes(10))
            );
        }

        @Test
        @DisplayName("캐시 키에 limit이 포함되어 정확히 반환되어야 한다")
        void whenCacheHitWithLimit_shouldReturnExactData() throws JsonProcessingException {
            // given
            String source = "all";
            String type = "all";
            int limit = 2;

            List<KeywordItem> cachedKeywords = List.of(
                    new KeywordItem("키워드1", "PERSON", 100),
                    new KeywordItem("키워드2", "PERSON", 80)
            );
            String cachedJson = objectMapper.writeValueAsString(cachedKeywords);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("keywords:today:source:all:type:all:limit:2")).willReturn(cachedJson);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(2);
            assertThat(response.metadata().totalCount()).isEqualTo(2);
            assertThat(response.metadata().limit()).isEqualTo(2);
            verify(elasticsearchService, never()).getKeywords(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("빈 캐시 데이터 시 Elasticsearch에서 조회해야 한다")
        void whenEmptyCacheData_shouldQueryElasticsearch() throws JsonProcessingException {
            // given
            String source = "news";
            String type = "LOCATION";
            int limit = 10;

            String emptyListJson = "[]";
            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("서울", "LOCATION", 200)
            );

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn(emptyListJson);
            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(1);
            verify(elasticsearchService).getKeywords(source, type, limit);
        }

        @Test
        @DisplayName("Redis 역직렬화 실패 시 Elasticsearch에서 조회해야 한다")
        void whenRedisDeserializationFails_shouldQueryElasticsearch() {
            // given
            String source = "news";
            String type = "PERSON";
            int limit = 10;

            List<KeywordItem> esKeywords = List.of(
                    new KeywordItem("테스트", "PERSON", 50)
            );

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn("invalid json {{{");
            given(elasticsearchService.getKeywords(source, type, limit)).willReturn(esKeywords);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.keywords()).hasSize(1);
            verify(elasticsearchService).getKeywords(source, type, limit);
        }

        @Test
        @DisplayName("메타데이터가 올바르게 생성되어야 한다")
        void shouldCreateCorrectMetadata() throws JsonProcessingException {
            // given
            String source = "news";
            String type = "ORGANIZATION";
            int limit = 10;

            List<KeywordItem> cachedKeywords = List.of(
                    new KeywordItem("삼성전자", "ORGANIZATION", 150)
            );
            String cachedJson = objectMapper.writeValueAsString(cachedKeywords);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn(cachedJson);

            // when
            var response = keywordService.getKeywords(source, type, limit);

            // then
            assertThat(response.metadata().totalCount()).isEqualTo(1);
            assertThat(response.metadata().limit()).isEqualTo(limit);
            assertThat(response.metadata().source()).isEqualTo(source);
            assertThat(response.metadata().type()).isEqualTo(type);
            assertThat(response.metadata().lastUpdated()).isNotNull();
        }
    }
}
